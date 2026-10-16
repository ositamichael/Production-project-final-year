// Isolated logic tests, not evidence of real microphones, speakers or a live TURN server.
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import test from 'node:test';
import assert from 'node:assert/strict';

const source = readFileSync(new URL('../src/main/resources/static/js/app.js', import.meta.url), 'utf8');
function section(start, end) {
  const first = source.indexOf(start);
  const last = source.indexOf(end, first);
  assert.ok(first >= 0 && last > first, `Missing source boundaries: ${start}`);
  return source.slice(first, last);
}
const configurationCode = section('async function loadCallConfiguration()', 'function waitForRealtime()');
const audioCode = section('async function playRemoteCallAudio(', 'async function createCallPeer(');
const peerCode = section('async function createCallPeer(', 'function startCallConnectionDeadline(');
const stopCode = section('function stopCallMedia(', 'function finishCall(');
const candidateCode = section('async function queueLocalCallCandidate(', 'function updateCallControl(');
const requestCode = section('async function requestCallMedia(', 'async function playRemoteCallAudio(');
const answerCode = section('async function acceptIncomingCall(', 'async function handleCallSignal(');
const signalCode = section('async function handleCallSignal(', 'async function declineOrEndCall(');

class FakeStream {
  constructor(tracks = []) { this.tracks = [...tracks]; }
  getTracks() { return this.tracks; }
  getAudioTracks() { return this.tracks.filter(track => track.kind === 'audio'); }
  addTrack(track) { this.tracks.push(track); }
}
class FakePeer {
  constructor(configuration) { this.configuration = configuration; this.listeners = {}; }
  addTrack() {}
  addEventListener(name, listener) { this.listeners[name] = listener; }
  close() { this.closed = true; }
}
function setup(extra = {}) {
  const context = vm.createContext({
    activeCall: null, callConfiguration: null, MediaStream: FakeStream, RTCPeerConnection: FakePeer,
    remoteCallAudio: { srcObject: null, play: async () => {}, pause() {} },
    remoteCallVideo: { srcObject: null, play: async () => {}, controls: false },
    localCallVideo: {}, callEnableSound: { hidden: true }, callAudioNotice: { hidden: true },
    callNetworkNotice: {}, callPeerAvatar: {}, callStatus: {}, clearTimeout() {},
    AbortController, window: { setTimeout() {} },
    callAccept: {}, callDecline: {}, callMute: {}, callCamera: {},
    incomingCallActions: {}, activeCallActions: {},
    sendCallSignal: async () => {}, finishCall() {}, ...extra
  });
  vm.runInContext(audioCode + peerCode + stopCode + candidateCode, context);
  return context;
}

test('configuration is fetched afresh for every call and requests no-store', async () => {
  let requests = 0;
  const context = setup({ fetch: async (url, options) => {
    assert.equal(url, '/api/social/calls/configuration');
    assert.equal(options.cache, 'no-store');
    requests++;
    return { ok: true, json: async () => ({ iceServers: [], relayConfigured: true, iceTransportPolicy: 'relay' }) };
  } });
  vm.runInContext(configurationCode, context);
  assert.equal((await context.loadCallConfiguration()).iceTransportPolicy, 'relay');
  await context.loadCallConfiguration();
  assert.equal(requests, 2);
});

test('unavailable free relay gives an actionable error instead of silently claiming a connection', async () => {
  const context = setup({ fetch: async () => ({ ok: false, status: 503 }) });
  vm.runInContext(configurationCode, context);
  await assert.rejects(context.loadCallConfiguration(), /free allowance or settings/);
});

test('a stalled call configuration request is aborted and explained', async () => {
  let timeout;
  const context = setup({ window: { setTimeout: (callback, delay) => {
    assert.equal(delay, 10000);
    timeout = callback;
  } }, fetch: async (url, options) => new Promise((resolve, reject) => {
    options.signal.addEventListener('abort', () => {
      const error = new Error('aborted');
      error.name = 'AbortError';
      reject(error);
    });
  }) });
  vm.runInContext(configurationCode, context);
  const pending = context.loadCallConfiguration();
  timeout();
  await assert.rejects(pending, /Check your internet connection/);
});

test('an expired sign-in is not misreported as a network or relay failure', async () => {
  const context = setup({ fetch: async () => ({ ok: false, status: 401 }) });
  vm.runInContext(configurationCode, context);
  await assert.rejects(context.loadCallConfiguration(), /sign-in expired/);
});

test('blocked speaker playback shows an actionable button and a successful retry hides it', async () => {
  const context = setup();
  const call = {};
  context.activeCall = call;
  context.remoteCallAudio.srcObject = new FakeStream();
  context.remoteCallAudio.play = async () => { throw new Error('NotAllowedError'); };
  await context.playRemoteCallAudio(call);
  assert.equal(context.callEnableSound.hidden, false);
  assert.match(context.callAudioNotice.textContent, /Enable call sound/);
  context.remoteCallAudio.play = async () => {};
  await context.playRemoteCallAudio(call);
  assert.equal(context.callEnableSound.hidden, true);
  assert.equal(context.callAudioNotice.hidden, true);
});

test('a playback result from an ended call cannot change a newer call UI', async () => {
  const context = setup();
  const oldCall = {};
  context.activeCall = oldCall;
  context.remoteCallAudio.srcObject = new FakeStream();
  let rejectPlayback;
  context.remoteCallAudio.play = () => new Promise((resolve, reject) => { rejectPlayback = reject; });
  const pending = context.playRemoteCallAudio(oldCall);
  context.activeCall = {};
  rejectPlayback(new Error('blocked'));
  await pending;
  assert.equal(context.callEnableSound.hidden, true);
});

test('streamless audio and video tracks accumulate without losing audio or duplicating it', async () => {
  const context = setup({ loadCallConfiguration: async () => ({
    iceServers: [{ urls: ['turn:relay.test:3478'] }], relayConfigured: true,
    iceTransportPolicy: 'relay', privacyNotice: 'Test only'
  }) });
  const call = { media: 'VIDEO', localStream: new FakeStream() };
  context.activeCall = call;
  const peer = await context.createCallPeer(call);
  assert.equal(peer.configuration.iceTransportPolicy, 'relay');
  const audio = { id: 'audio-1', kind: 'audio' };
  const video = { id: 'video-1', kind: 'video' };
  peer.listeners.track({ track: audio, streams: [] });
  peer.listeners.track({ track: video, streams: [] });
  peer.listeners.track({ track: audio, streams: [] });
  assert.equal(call.remoteStream.getTracks().length, 2);
  assert.deepEqual(context.remoteCallAudio.srcObject.getTracks(), [audio]);
  assert.equal(context.remoteCallVideo.srcObject, call.remoteStream);
  assert.equal(context.remoteCallVideo.hidden, false);
});

test('ending a call stops capture, closes the peer and detaches the audio player', () => {
  const context = setup();
  let stopped = false;
  const peer = new FakePeer({});
  context.remoteCallAudio.srcObject = new FakeStream();
  context.callEnableSound.hidden = false;
  context.stopCallMedia({ localStream: new FakeStream([{ stop() { stopped = true; } }]), peerConnection: peer });
  assert.equal(stopped, true);
  assert.equal(peer.closed, true);
  assert.equal(context.remoteCallAudio.srcObject, null);
  assert.equal(context.callEnableSound.hidden, true);
});

test('network candidates wait until the offer or answer has been sent', async () => {
  const sent = [];
  const context = setup({ sendCallSignal: async (type, details) => { sent.push(type); } });
  const call = { descriptionSignalled: false, pendingLocalCandidates: [], localCandidateChain: Promise.resolve() };
  context.activeCall = call;
  await context.queueLocalCallCandidate(call, { candidate: 'early' });
  assert.equal(sent.length, 0);
  await context.signalCallDescription('CALL_ANSWER', { sdp: 'answer' }, call);
  await context.queueLocalCallCandidate(call, { candidate: 'later' });
  assert.deepEqual(sent, ['CALL_ANSWER', 'CALL_ICE_CANDIDATE', 'CALL_ICE_CANDIDATE']);
});

test('cancelled calls do not send their buffered network candidates', async () => {
  const sent = [];
  const context = setup({ sendCallSignal: async (type) => {
    sent.push(type);
    context.activeCall = null;
  } });
  const call = { descriptionSignalled: false, pendingLocalCandidates: [{ candidate: 'early' }],
    localCandidateChain: Promise.resolve() };
  context.activeCall = call;
  await context.signalCallDescription('CALL_OFFER', { sdp: 'offer' }, call);
  assert.deepEqual(sent, ['CALL_OFFER']);
});

test('a permission prompt times out clearly and late capture is stopped', async () => {
  let timeout;
  let deliverStream;
  let stopped = false;
  const capture = new Promise(resolve => { deliverStream = resolve; });
  const context = setup({
    navigator: { mediaDevices: { getUserMedia: () => capture } },
    window: { isSecureContext: true, setTimeout: (callback, delay) => {
      assert.equal(delay, 25000);
      timeout = callback;
    } }
  });
  vm.runInContext(requestCode, context);
  const pending = context.requestCallMedia('AUDIO');
  timeout();
  await assert.rejects(pending, /permission is still waiting/);
  deliverStream(new FakeStream([{ stop() { stopped = true; } }]));
  await Promise.resolve();
  assert.equal(stopped, true);
});

test('permission denial explains how to retry rather than silently ringing', async () => {
  const context = setup({
    navigator: { mediaDevices: { getUserMedia: async () => { throw { name: 'NotAllowedError' }; } } },
    window: { isSecureContext: true, setTimeout() {} }
  });
  vm.runInContext(requestCode, context);
  await assert.rejects(context.requestCallMedia('VIDEO'), /Allow it in this site's browser settings/);
});

test('Accept acknowledges immediately, remains cancellable and requests capture only once', async () => {
  const sent = [];
  let captureRequests = 0;
  const context = setup({ requestCallMedia: () => { captureRequests++; return new Promise(() => {}); },
    sendCallSignal: async type => { sent.push(type); } });
  vm.runInContext(answerCode, context);
  context.activeCall = { direction: 'incoming', phase: 'ringing', media: 'AUDIO' };
  void context.acceptIncomingCall();
  void context.acceptIncomingCall();
  assert.equal(context.activeCall.phase, 'answering');
  assert.equal(context.incomingCallActions.hidden, true);
  assert.equal(context.activeCallActions.hidden, false);
  assert.equal(context.callMute.disabled, true);
  assert.equal(captureRequests, 1);
  assert.deepEqual(sent, ['CALL_ACCEPTING']);
});

test('caller stops showing Ringing as soon as the receiver taps Accept', async () => {
  let deadlineStarted = false;
  const context = setup({ startCallConnectionDeadline: () => { deadlineStarted = true; } });
  vm.runInContext(signalCode, context);
  context.activeCall = { id: 'test-id', direction: 'outgoing', phase: 'ringing' };
  await context.handleCallSignal({ type: 'CALL_ACCEPTING', callId: 'test-id' });
  assert.equal(context.activeCall.phase, 'answering');
  assert.match(context.callStatus.textContent, /Answered/);
  assert.equal(deadlineStarted, true);
});

test('an ended call cannot create a peer after a slow configuration response', async () => {
  const context = setup({ loadCallConfiguration: async () => ({ iceServers: [] }) });
  const call = { localStream: new FakeStream() };
  context.activeCall = null;
  await assert.rejects(context.createCallPeer(call), /cancelled/);
  assert.equal(call.peerConnection, undefined);
});

test('cleaning up a late stream never detaches a newer call audio stream', () => {
  const context = setup();
  const newerStream = new FakeStream();
  context.activeCall = {};
  context.remoteCallAudio.srcObject = newerStream;
  let stopped = false;
  context.stopCallMedia({ localStream: new FakeStream([{ stop() { stopped = true; } }]) });
  assert.equal(stopped, true);
  assert.equal(context.remoteCallAudio.srcObject, newerStream);
});
