package uk.ac.unicircle.assistant;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small, deterministic knowledge assistant that keeps the staging service useful
 * when no paid model provider is configured. It is deliberately honest about its
 * limits and never presents generated templates as live or verified research.
 */
@Component
class BasicAssistantEngine {
    private static final String BASIC_NOTICE =
            "Basic offline guidance from UniCircle. It does not search the live web; check important facts with an official source.";
    private static final Pattern CALCULATION = Pattern.compile(
            "(?:what is|calculate|work out)?\\s*(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/x×÷])\\s*(-?\\d+(?:\\.\\d+)?)\\s*[?]?",
            Pattern.CASE_INSENSITIVE);

    Optional<AssistantResponses.Answer> answerIfKnown(AssistantMode mode, String message) {
        String question = message.trim();
        String normal = question.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+\\-*/x×÷. ]", " ")
                .replaceAll("\\s+", " ").trim();

        if (containsAny(normal, "password", "authenticator code", "one time code", "otp code")) {
            return Optional.of(response(mode, "Never share a password or Authenticator code with UniCircle. Use only the university's official sign-in page, and contact the university IT service if you think an account may be at risk."));
        }
        if (containsAny(normal, "write my assignment", "do my assignment", "complete my assignment", "cheat")) {
            return Optional.of(response(mode, "I cannot complete assessed work for you. I can help you understand the question, create a plan, explain difficult ideas, review your draft and prepare practice questions so the final work remains yours."));
        }

        Matcher calculation = CALCULATION.matcher(normal);
        if (calculation.matches()) {
            return Optional.of(response(mode, calculate(calculation)));
        }

        if (containsAny(normal, "best programming language", "best coding language")) {
            return Optional.of(response(mode, "There is no single best programming language for every job. Python is approachable for beginners, automation and data work; Java and C# are strong for large business systems; JavaScript or TypeScript is central to interactive web development; C and C++ suit systems and performance-critical work; and Kotlin or Swift are common choices for native mobile development. Choose by the problem, available libraries, deployment platform, team skills and maintainability rather than popularity alone."));
        }

        if (mode == AssistantMode.IMAGE_STUDIO) {
            return Optional.of(imageResponse(question));
        }

        String answer = switch (mode) {
            case GENERAL -> generalAnswer(normal);
            case UNIVERSITY_RESEARCH -> universityAnswer(normal);
            case STUDY_COACH -> studyAnswer(normal);
            case CODE_LAB -> codeAnswer(normal);
            case IMAGE_STUDIO -> null;
        };
        return answer == null ? Optional.empty() : Optional.of(response(mode, answer));
    }

    private AssistantResponses.Answer response(AssistantMode mode, String answer) {
        return new AssistantResponses.Answer(answer, List.of(), null, mode, false, BASIC_NOTICE);
    }

    private String generalAnswer(String question) {
        if (isGreeting(question)) {
            return "Hello! I am UniCircle's basic offline assistant. I can explain common computing topics, help you plan study, give introductory Spring Boot and programming guidance, explain UniCircle features and perform simple calculations.";
        }
        if (containsAny(question, "what is a computer", "what is computer", "define computer")) {
            return "A computer is an electronic device that accepts data, follows stored instructions, processes the data and produces useful output. Its main parts are input devices, a processor, memory or storage, and output devices. For example, when you type a document, the keyboard supplies input, the processor follows the program's instructions, storage keeps the file and the screen displays the result.";
        }
        if (containsAny(question, "artificial intelligence", "what is ai", "define ai")) {
            return "Artificial intelligence is the field of creating computer systems that perform tasks normally associated with human intelligence, such as recognising patterns, understanding language, making predictions or recommending actions. AI does not automatically understand everything and its answers must be tested for accuracy and fairness.";
        }
        if (containsAny(question, "machine learning", "what is ml")) {
            return "Machine learning is a part of AI in which a system learns patterns from examples instead of being given every rule by a programmer. A typical project collects data, prepares it, trains a model, tests it on unseen data and measures how well it performs.";
        }
        if (containsAny(question, "algorithm")) {
            return "An algorithm is a clear sequence of steps used to solve a problem. A recipe is a simple everyday example. In software, an algorithm should have defined inputs, ordered steps, an output and a point where it finishes.";
        }
        if (containsAny(question, "database")) {
            return "A database is an organised collection of data that software can store, search and update. A relational database keeps information in linked tables. UniCircle, for example, uses database records for accounts, connection requests, messages, bookings and campus circles.";
        }
        if (containsAny(question, "api", "application programming interface")) {
            return "An API is a controlled way for software systems to communicate. The client sends a request to an agreed address, the server checks it and returns a structured response. UniCircle's web interface uses APIs to request messages, events and profile information from its Spring Boot server.";
        }
        if (containsAny(question, "cloud computing", "what is cloud")) {
            return "Cloud computing means using computing resources delivered over a network instead of owning every server yourself. Common services include hosted applications, databases, file storage and virtual servers. Render currently hosts the UniCircle staging service in this way.";
        }
        if (containsAny(question, "operating system", "what is os")) {
            return "An operating system manages a computer's hardware and provides services for applications. It handles memory, files, devices, processes, security and the user interface. macOS, Windows, Linux, Android and iOS are operating systems.";
        }
        if (containsAny(question, "cybersecurity", "cyber security")) {
            return "Cybersecurity is the practice of protecting systems, networks and data from unauthorised access, damage or disruption. Important controls include strong authentication, least privilege, secure updates, encryption, backups, logging and regular testing.";
        }
        if (containsAny(question, "unicircle", "find student", "friend request", "message student")) {
            return "UniCircle is a university-community platform. Sign in with a staging account, open Messages and choose Find students to discover another test account. Send a connection request; after the other person accepts it, private messaging becomes available. Location sharing remains off until each accepted connection chooses to enable it.";
        }
        return null;
    }

    private String universityAnswer(String question) {
        if (containsAny(question, "leeds beckett", "my university", "university information")) {
            return "This pilot is not connected to Leeds Beckett University's private systems. For confirmed course, fee, admissions, timetable or policy information, use the university's official website or student portal. UniCircle can later index approved university pages and documents so answers include a traceable source.";
        }
        if (containsAny(question, "compare universities", "choose a university")) {
            return "Compare universities using the same evidence for each one: course modules, entry requirements, accreditation, total cost, location, student support, placement opportunities and independently published outcomes. Record the official page and date for every claim instead of relying on rankings alone.";
        }
        if (containsAny(question, "student visa", "visa")) {
            return "Student-visa rules can change, so this offline assistant should not give a final eligibility decision. Check the current UK Government Student visa guidance and confirm any university sponsorship requirement with the institution's international-student team.";
        }
        if (containsAny(question, "fees", "tuition")) {
            return "Tuition fees differ by course, study year and fee status. Use the exact course page for the intended intake, then confirm what the fee includes, payment dates, scholarships and any additional course costs with the university.";
        }
        return null;
    }

    private String studyAnswer(String question) {
        if (containsAny(question, "agile", "sprint")) {
            return "Agile development builds software in short, reviewable stages. For UniCircle, each sprint should select a realistic group of features, implement and test them on a separate Git branch, collect evidence, review the result and merge only when the acceptance criteria pass.";
        }
        if (containsAny(question, "moscow")) {
            return "MoSCoW is a prioritisation method: Must Have requirements are essential; Should Have requirements are important but not essential for the first release; Could Have requirements are useful if time allows; and Won't Have for now records ideas deliberately postponed.";
        }
        if (containsAny(question, "risk register", "project risk")) {
            return "A risk register records what might go wrong, how likely it is, its impact, who owns it and what will reduce it. Useful UniCircle risks include unavailable university integrations, privacy failures, unreliable calls, inaccurate AI answers, insufficient testing and an over-ambitious scope.";
        }
        if (containsAny(question, "unit test", "unit testing")) {
            return "A unit test checks one small part of a program automatically. Arrange the required data, act by calling the behaviour, and assert the expected result. Good tests cover normal behaviour, boundary values, invalid input and important failure cases.";
        }
        if (containsAny(question, "version control", "git")) {
            return "Version control records changes to a project so work can be reviewed and recovered. A clear student workflow is: create a branch for one sprint or feature, make focused commits, run tests, push the branch, review the difference and then merge it into the main branch.";
        }
        if (containsAny(question, "debugger", "debugging")) {
            return "A debugger lets you pause a running program, inspect values and move through the code one line at a time. Start by reproducing the problem, set a breakpoint before the suspected failure, inspect the inputs and follow the program until the observed value differs from what you expected.";
        }
        if (containsAny(question, "final year project", "project aim", "project objective")) {
            return "A strong final-year-project aim states the problem, intended users and measurable outcome in one sentence. Objectives then describe the research, design, implementation, testing and evaluation needed to achieve that aim. Each objective should produce evidence that an assessor can inspect.";
        }
        if (containsAny(question, "help me study", "help me practise", "study plan", "revision plan")) {
            return "Use this study method: first state what you already understand; split the topic into three small questions; explain each answer in your own words with one example; then test yourself without notes. Tell me the exact topic and your current understanding, and I can help structure those steps using my available offline knowledge.";
        }
        return null;
    }

    private String codeAnswer(String question) {
        if (containsAny(question, "spring boot")) {
            return "Spring Boot is a Java framework that reduces the setup needed for web applications. A common structure uses controllers for HTTP requests, services for business rules, repositories for database access and configuration classes for security or external services. Keep each responsibility separate and test the service and controller behaviour.";
        }
        if (containsAny(question, "what is programming", "define programming")) {
            return "Programming is the process of designing and writing instructions that a computer can follow. A programmer understands a problem, creates an algorithm, expresses it in a language such as Python, Java or C#, tests the result and corrects errors.";
        }
        if (containsAny(question, "what is python", "define python", "python programming language")) {
            return "Python is a high-level programming language designed to be readable and quick to develop with. It is widely used for web development, automation, data analysis, artificial intelligence and teaching. Python uses indentation to group code. A simple example is: print(\"Hello, world!\").";
        }
        if (containsAny(question, "what is java", "define java", "java programming language")) {
            return "Java is an object-oriented programming language used for web back ends, Android-related development, enterprise systems and many other applications. Java code is compiled to bytecode that runs on the Java Virtual Machine, which helps the same program run on different operating systems. Spring Boot is a popular Java framework.";
        }
        if (containsAny(question, "what is c#", "what is c sharp", "define c#", "define c sharp")) {
            return "C# is a modern object-oriented programming language created for the .NET platform. It is used for desktop applications, web services, cloud systems and games made with Unity. A simple output statement is: Console.WriteLine(\"Hello, world!\");";
        }
        if (containsAny(question, "what is javascript", "define javascript")) {
            return "JavaScript is a programming language mainly used to make web pages interactive. It runs in browsers and can also run on servers through environments such as Node.js. It can respond to clicks, update page content, validate forms and communicate with an API.";
        }
        if (containsAny(question, "java for loop", "for loop in java", "java loop")) {
            return "A Java for loop repeats code while a condition remains true. Example: for (int i = 0; i < 5; i++) { System.out.println(i); } This starts i at 0, runs while i is below 5 and adds 1 after each repetition, so it prints 0 to 4.";
        }
        if (containsAny(question, "c# for loop", "for loop in c#", "c sharp loop")) {
            return "A C# for loop repeats a block of code. Example: for (int i = 0; i < 5; i++) { Console.WriteLine(i); } It starts at 0, continues while i is below 5 and increases i by 1 each time.";
        }
        if (containsAny(question, "python for loop", "for loop in python", "python loop")) {
            return "A Python for loop visits each value in a sequence. Example: for number in range(5): print(number). With the print line indented, this prints 0 to 4. Python uses indentation to show which statements belong inside the loop.";
        }
        if (containsAny(question, "method", "function")) {
            return "A function or method is a named, reusable block of instructions. It can receive inputs called parameters, perform one job and optionally return a result. Keep it small, give it a clear name and test normal, boundary and invalid inputs.";
        }
        if (containsAny(question, "array", "list")) {
            return "An array or list stores several related values under one name. Arrays normally have a fixed size, while list collections can usually grow and shrink. Choose a meaningful element type, remember that many languages start indexing at 0, and check bounds before accessing an index.";
        }
        if (containsAny(question, "what is sql", "define sql")) {
            return "SQL means Structured Query Language. It is used to create, read, update and delete information in relational databases. For example, SELECT name FROM students; reads the name column from the students table.";
        }
        if (containsAny(question, "sql", "select query", "database query")) {
            return "SQL is used to work with relational databases. A basic read is: SELECT name, course FROM students WHERE active = true ORDER BY name; Use parameterised queries rather than joining user input into SQL, define keys and constraints, and test that users can access only the records they are permitted to see.";
        }
        if (containsAny(question, "rest api", "restful", "http endpoint")) {
            return "A REST API exposes resources through HTTP. GET reads data, POST creates it, PUT or PATCH updates it and DELETE removes it. Validate every request, return suitable status codes, keep business rules in a service layer and require authorisation for protected resources.";
        }
        if (containsAny(question, "html", "css", "javascript")) {
            return "HTML gives a page its meaning and structure, CSS controls its presentation, and JavaScript adds behaviour. Start with semantic HTML, create responsive CSS that works on small screens, then add JavaScript only for interactions. Keep accessibility features such as labels, keyboard focus and sufficient colour contrast.";
        }
        if (containsAny(question, "unit test", "junit", "xunit", "test code")) {
            return "A code unit test follows Arrange, Act, Assert: prepare the input, call one behaviour and compare the result with what should happen. Include a normal case, boundary case and invalid case. In Java, JUnit is common; in C#, xUnit, NUnit and MSTest are common choices.";
        }
        if (containsAny(question, "object oriented", "oop", "inheritance", "interface")) {
            return "Object-oriented programming organises software around objects that combine data and behaviour. Encapsulation protects internal state, inheritance can reuse or specialise behaviour, polymorphism allows one interface to have several implementations, and abstraction exposes what a component does without revealing every internal detail. Prefer interfaces when components need replaceable implementations.";
        }
        if (containsAny(question, "nullpointerexception", "null pointer")) {
            return "A NullPointerException means Java tried to use an object reference that currently contains null. Read the first line in your own code from the stack trace, inspect each object used on that line, find why it was not created or supplied, and fix the source of the missing value rather than hiding the problem with a broad catch block.";
        }
        if (containsAny(question, "error", "bug", "debug")) {
            return "To debug a program: reproduce the problem consistently, copy the exact error and stack trace, identify the first relevant line in your code, inspect the inputs with a debugger, make one small correction and add a test that would fail if the bug returned. Share the language, exact error and smallest relevant code section for more focused help.";
        }
        if (containsAny(question, "class", "object")) {
            return "A class is a blueprint that defines data and behaviour; an object is one actual instance created from that blueprint. For example, Student can be a class with name and course fields, while Michael's student record is an object of that class.";
        }
        return null;
    }

    private AssistantResponses.Answer imageResponse(String prompt) {
        String cleanPrompt = prompt.replaceAll("\\s+", " ").trim();
        if (cleanPrompt.isBlank()) cleanPrompt = "A welcoming university community";
        String[] palette = illustrationPalette(cleanPrompt.hashCode());
        String[] title = illustrationTitle(cleanPrompt);
        String scene = illustrationScene(cleanPrompt.toLowerCase(Locale.ROOT), palette);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800" role="img" aria-labelledby="title description">
                  <title id="title">%s</title>
                  <desc id="description">Original procedural illustration created in UniCircle Image Studio.</desc>
                  <defs>
                    <linearGradient id="background" x1="0" y1="0" x2="1" y2="1"><stop stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient>
                    <linearGradient id="card" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#ffffff" stop-opacity=".98"/><stop offset="1" stop-color="#eff6ff" stop-opacity=".94"/></linearGradient>
                    <filter id="shadow" x="-20%%" y="-20%%" width="140%%" height="160%%"><feDropShadow dx="0" dy="18" stdDeviation="24" flood-color="#061b3a" flood-opacity=".2"/></filter>
                  </defs>
                  <rect width="1200" height="800" rx="48" fill="url(#background)"/>
                  <circle cx="1080" cy="80" r="220" fill="%s" opacity=".18"/><circle cx="80" cy="760" r="250" fill="%s" opacity=".16"/>
                  <path d="M0 630C190 550 315 700 520 620s330-150 680-36v216H0Z" fill="#061b3a" opacity=".16"/>
                  %s
                  <g filter="url(#shadow)"><rect x="76" y="70" width="620" height="222" rx="32" fill="url(#card)"/></g>
                  <text x="116" y="132" fill="%s" font-family="Inter,Arial,sans-serif" font-size="24" font-weight="800" letter-spacing="4">UNICIRCLE ORIGINAL</text>
                  <text x="116" y="202" fill="#092551" font-family="Inter,Arial,sans-serif" font-size="46" font-weight="850">%s</text>
                  <text x="116" y="258" fill="#092551" font-family="Inter,Arial,sans-serif" font-size="46" font-weight="850">%s</text>
                  <g transform="translate(1050 700)"><circle r="54" fill="#ffffff" opacity=".95"/><circle r="31" fill="none" stroke="#2ad4cf" stroke-width="13"/><path d="M21-23A31 31 0 0 1 31 0" fill="none" stroke="#2168ed" stroke-width="13" stroke-linecap="round"/><circle r="7" fill="#092551"/></g>
                  <text x="78" y="748" fill="#ffffff" font-family="Inter,Arial,sans-serif" font-size="22" font-weight="700">Created locally · private by design · editable vector artwork</text>
                </svg>
                """.formatted(
                xml(cleanPrompt), palette[0], palette[1], palette[2], palette[3], scene,
                palette[4], xml(title[0]), xml(title[1]));
        String dataUrl = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new AssistantResponses.Answer(
                "I created a downloadable vector illustration from your description. This free mode is designed for clean posters, banners and concept graphics. It does not pretend to be a photorealistic, unlimited AI image model.",
                List.of(), dataUrl, AssistantMode.IMAGE_STUDIO, false,
                "Created locally from UniCircle drawing rules. Your prompt was not sent to an external image service.");
    }

    private String[] illustrationPalette(int hash) {
        String[][] palettes = {
                {"#082b59", "#2568d9", "#2ad4cf", "#7ee7d8", "#2168ed"},
                {"#38246f", "#7457ed", "#ff8ab6", "#ffc76b", "#6b55e8"},
                {"#0c5f55", "#22a879", "#7ee0b5", "#f7ca62", "#117c6c"},
                {"#71344d", "#db5f73", "#ffb06b", "#ffe39a", "#bb3d64"}
        };
        return palettes[Math.floorMod(hash, palettes.length)];
    }

    private String[] illustrationTitle(String prompt) {
        String title = prompt.replaceFirst("(?i)^(create|generate|make|design|draw|illustrate)\\s+(an?\\s+)?", "")
                .replaceFirst("[.!?]+$", "").trim();
        if (title.length() > 72) title = title.substring(0, 69).stripTrailing() + "…";
        String[] words = title.split(" ");
        int split = words.length <= 5 ? words.length : Math.min(5, (words.length + 1) / 2);
        String first = String.join(" ", java.util.Arrays.copyOfRange(words, 0, split));
        String second = split < words.length
                ? String.join(" ", java.util.Arrays.copyOfRange(words, split, words.length)) : "";
        return new String[]{first, second};
    }

    private String illustrationScene(String prompt, String[] palette) {
        if (containsAny(prompt, "code", "computer", "technology", "software", "ai", "robot")) {
            return """
                    <g transform="translate(720 250)"><rect x="0" y="0" width="360" height="240" rx="28" fill="#ffffff" opacity=".96"/><rect x="25" y="25" width="310" height="160" rx="15" fill="#092551"/><path d="m95 86-34 28 34 28M265 86l34 28-34 28M210 64l-57 105" fill="none" stroke="%s" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/><path d="M125 240h110M180 185v55" stroke="#ffffff" stroke-width="18" stroke-linecap="round"/><circle cx="42" cy="215" r="35" fill="%s"/><circle cx="318" cy="215" r="35" fill="%s"/></g>
                    """.formatted(palette[2], palette[3], palette[4]);
        }
        if (containsAny(prompt, "library", "book", "study", "class", "student", "university", "campus")) {
            return """
                    <g transform="translate(720 235)"><rect x="0" y="0" width="400" height="300" rx="30" fill="#ffffff" opacity=".94"/><path d="M35 88h330M35 150h330M35 212h330" stroke="#c9d9ed" stroke-width="10"/><g fill="%s"><rect x="55" y="35" width="28" height="48" rx="4"/><rect x="89" y="48" width="22" height="35" rx="4"/><rect x="278" y="101" width="31" height="44" rx="4"/><rect x="315" y="92" width="24" height="53" rx="4"/></g><ellipse cx="200" cy="248" rx="122" ry="24" fill="#d7e5f4"/><circle cx="127" cy="199" r="31" fill="%s"/><circle cx="200" cy="184" r="31" fill="%s"/><circle cx="273" cy="199" r="31" fill="%s"/><path d="M105 244c8-37 37-42 50 0M175 235c8-37 42-40 52 0M250 244c8-37 37-42 50 0" fill="none" stroke="#092551" stroke-width="18" stroke-linecap="round"/></g>
                    """.formatted(palette[4], palette[2], palette[3], palette[4]);
        }
        return """
                <g transform="translate(735 210)"><circle cx="180" cy="180" r="170" fill="#ffffff" opacity=".9"/><path d="M55 238 180 78l125 160Z" fill="%s" opacity=".9"/><circle cx="180" cy="180" r="88" fill="%s" opacity=".9"/><path d="M70 298c58-74 162-74 220 0" fill="none" stroke="%s" stroke-width="34" stroke-linecap="round"/><circle cx="115" cy="260" r="36" fill="#ffffff"/><circle cx="245" cy="260" r="36" fill="#ffffff"/></g>
                """.formatted(palette[2], palette[3], palette[4]);
    }

    private String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String calculate(Matcher calculation) {
        BigDecimal left = new BigDecimal(calculation.group(1));
        BigDecimal right = new BigDecimal(calculation.group(3));
        String operator = calculation.group(2);
        BigDecimal result = switch (operator) {
            case "+" -> left.add(right);
            case "-" -> left.subtract(right);
            case "*", "x", "×" -> left.multiply(right);
            case "/", "÷" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) yield null;
                yield left.divide(right, 10, RoundingMode.HALF_UP).stripTrailingZeros();
            }
            default -> null;
        };
        if (result == null) return "Division by zero is undefined. Use a non-zero divisor.";
        return left.stripTrailingZeros().toPlainString() + " " + operator + " "
                + right.stripTrailingZeros().toPlainString() + " = " + result.stripTrailingZeros().toPlainString() + ".";
    }

    private boolean isGreeting(String question) {
        return question.matches("(hello|hi|hey|good morning|good afternoon|good evening)( there)?");
    }

    private boolean containsAny(String value, String... phrases) {
        for (String phrase : phrases) {
            if (value.contains(phrase)) return true;
        }
        return false;
    }
}
