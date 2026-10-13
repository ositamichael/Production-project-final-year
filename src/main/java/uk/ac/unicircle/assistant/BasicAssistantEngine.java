package uk.ac.unicircle.assistant;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
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

    AssistantResponses.Answer answer(AssistantMode mode, String message) {
        String question = message.trim();
        String normal = question.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+\\-*/x×÷. ]", " ")
                .replaceAll("\\s+", " ").trim();

        if (containsAny(normal, "password", "authenticator code", "one time code", "otp code")) {
            return response(mode, "Never share a password or Authenticator code with UniCircle. Use only the university's official sign-in page, and contact the university IT service if you think an account may be at risk.");
        }
        if (containsAny(normal, "write my assignment", "do my assignment", "complete my assignment", "cheat")) {
            return response(mode, "I cannot complete assessed work for you. I can help you understand the question, create a plan, explain difficult ideas, review your draft and prepare practice questions so the final work remains yours.");
        }

        Matcher calculation = CALCULATION.matcher(normal);
        if (calculation.matches()) {
            return response(mode, calculate(calculation));
        }

        String answer = switch (mode) {
            case GENERAL -> generalAnswer(normal);
            case UNIVERSITY_RESEARCH -> universityAnswer(normal);
            case STUDY_COACH -> studyAnswer(normal);
            case CODE_LAB -> codeAnswer(normal);
            case IMAGE_STUDIO -> imageAnswer(question);
        };
        return response(mode, answer);
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
        return "I do not yet have a reliable offline answer for that question. Try asking about computers, AI, algorithms, databases, APIs, cloud computing, cybersecurity, Spring Boot, testing, Agile development, final-year-project planning or how to use UniCircle. Live research and unrestricted questions need a larger locally hosted model or an approved AI provider.";
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
        return "For university research, write down the exact institution, course, intake year and question. Check the institution's official course page first, then an appropriate government or regulator source. This basic offline assistant cannot verify changing admissions, fees, visa, deadline or policy information live.";
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
        return "Use this study method: first state what you already understand; split the topic into three small questions; explain each answer in your own words with one example; then test yourself without notes. Tell me the exact topic and your current understanding, and I can help structure those steps using my available offline knowledge.";
    }

    private String codeAnswer(String question) {
        if (containsAny(question, "spring boot")) {
            return "Spring Boot is a Java framework that reduces the setup needed for web applications. A common structure uses controllers for HTTP requests, services for business rules, repositories for database access and configuration classes for security or external services. Keep each responsibility separate and test the service and controller behaviour.";
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
        return "I can currently help with beginner Java, C#, Python, SQL, HTML, CSS, JavaScript, Spring Boot, REST APIs, object-oriented programming, debugging and testing. Include the language, intended result, actual result, exact error and the smallest relevant code section. I cannot reliably analyse a large unfamiliar program in basic offline mode. Do not include passwords, API keys or personal data.";
    }

    private String imageAnswer(String prompt) {
        return "The no-cost basic assistant cannot render an original image yet, but it can improve your image brief. Suggested brief: “"
                + prompt.trim()
                + "”. Add the subject, setting, composition, lighting, colour palette, visual style, aspect ratio and anything that must not appear. Local image generation can be added later on hardware with enough memory.";
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
