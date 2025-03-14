package no.uio.ifi.in4060.oblig;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// TODO: we use non-unique name assumption in semantic web, so task 2.9 IS ENTAILED -> CHANGE IT!!!
// TODO: write down in your notes the definitions and differences of soundness and completeness (it will be on the exam)
// entailment and inference are two different things

public class Oblig4 {
    private static Query query;
    private static Model resultModel;
    private static InfModel combinedModel;
    private static String serializationLanguage;

    private static String schemaPath;
    private static String queryPath;
    private static String outputPath;

    private static final StringBuilder stringBuilder = new StringBuilder();
    private static final String absolutePath = System.getProperty("user.dir") + "/src/main/java/no/uio/ifi/in4060/oblig/";
    private static final String dataPath = "https://www.uio.no/studier/emner/matnat/ifi/IN3060/v23/obliger/simpsons.ttl";

    public static void main(String[] args) {
        System.out.println("👋🏼 Welcome! We will find all people Homer Simpson has a family relation to... Now watch the magic 🧚🏼✨\n");

        if (args.length != 3) {
            System.err.println(
                    "🚫 Illegal arguments: please provide\n\t" +
                    "1: path to the RDF file\n\t" +
                    "2: path to the SPARQL construct query\n\t" +
                    "3: output file name"
            );
            System.exit(1);
        }

        String arg1 = args[0]; // path to the file your have written in the first exercise, RDFS modelling
        String arg2 = args[1]; // path to your SPARQL construct query
        String arg3 = args[2]; // file name to where the results of the SPARQL query shall be written
        System.out.println("ℹ️ Running program with arguments: " + arg1 + ", " + arg2 + " and " + arg3);

        schemaPath = absolutePath + arg1;
        queryPath = absolutePath + arg2;
        outputPath = absolutePath + arg3;
        validatePaths(arg1, arg2, arg3);

        try {
            if (isNotValidFileExtension(dataPath) || isNotValidFileExtension(schemaPath)) throw new IllegalArgumentException(
                    "🚫 Illegal arguments: RDF file and output file must be of type .rdf, .ttl, .n3 or .nt"
            );
            if (!queryPath.endsWith(".rq")) throw new IllegalArgumentException(
                    "🚫 Illegal arguments: SPARQL construct query file must be of type .rq"
            );
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            System.out.println("ℹ️ Testing...");
            List<String> familyStatements = testReadRDFFile(schemaPath);
            List<String> simpsonsStatements = testReadRDFFile(absolutePath + "simpsons.ttl");
            testListStatements("Family", familyStatements);
            testListStatements("Simpsons", simpsonsStatements);
        } catch (IOException e) {
            System.err.println("🚫 Something wrong happened while testing statements: " + e.getMessage());
            System.err.println("👁️👁️ But we don't really care about that 💅🏼 moving on ✨");
        }

        initCombinedModel(schemaPath, dataPath);
        initQuery(queryPath);
        executeQuery();
        writeOutputResults(outputPath);
        System.out.println("\n☺️All done! You can go check the generated FOAF file for Homer Simpson at: " + outputPath);
    }

    private static void writeOutputResults(String path) {
        System.out.println("\nℹ️ Writing output results...");
        try (FileOutputStream writer = new FileOutputStream(path)) {
            // redundant to set serialization language again for the arguments I use (both are turtle),
            // but setting it again just in case the teacher changes the arguments
            setSerializationLanguage(path, "");

            resultModel.write(writer, serializationLanguage);
            System.out.println("✅ Success!");
        } catch (IOException e) {
            throw new Error("🚫 Error writing output query results to file: " + e.getMessage());
        }
    }

    private static void executeQuery() {
        System.out.println("\nℹ️ Executing query...");
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, combinedModel)) {
            resultModel = queryExecution.execConstruct();
            System.out.println("✅ Success!");
        } catch (QueryParseException e) {
            throw new Error("🚫 Error executing query: " + e.getMessage());
        }
    }

    private static void initQuery(String path) {
        System.out.println("\nℹ️ Initiating query...");
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            stringBuilder.setLength(0); // reset just in case
            String queryString;
            String line;

            while((line = reader.readLine()) != null ) {
                stringBuilder.append(line).append("\n");
            }

            queryString = stringBuilder.toString();
            query = QueryFactory.create(queryString);
            System.out.println("✅ Success!");
        } catch (IOException e) {
            throw new Error("🚫 Error reading SPARQL file: " + e.getMessage());
        }
    }

    private static Model getModel(String uri) throws FileNotFoundException {
        System.out.println("\tℹ️ Getting model with uri: " + uri);
        Model model = ModelFactory.createDefaultModel();
        setSerializationLanguage(uri, "\t");

        if (uri.startsWith("http")) {
            model.read(uri, null, serializationLanguage);
        } else {
            InputStream inputStream = new FileInputStream(uri);
            model.read(inputStream, null, serializationLanguage);
        }

        System.out.println("\t✅ Success!");
        return model;
    }

    private static void initCombinedModel(String schemeURI, String dataURI) {
        System.out.println("\nℹ️ Initiating inferred model combined by schema and data model...");
        try {
            Model schema = getModel(schemeURI);
            Model data = getModel(dataURI);

            combinedModel = ModelFactory.createRDFSModel(schema, data);
            System.out.println("✅ Success!");
        } catch (FileNotFoundException e) {
            throw new Error("\n🚫 Error reading file: " + e.getMessage() + "\n➡️ Make sure you are in the right directory");
        }
    }

    private static void setSerializationLanguage(String uri, String pad) {
        if (uri.endsWith(".rdf")) {
            serializationLanguage = "RDF/XML";
        } else if (uri.endsWith(".ttl")) {
            serializationLanguage = "TURTLE";
        } else if (uri.endsWith(".n3")) {
            serializationLanguage = "N3";
        } else if (uri.endsWith(".nt")) {
            serializationLanguage = "N-TRIPLE";
        } else throw new Error(
                "🚫 Error getting serialization language, supported formats are: .rdf (RDF/XML), .ttl (TURTLE), .n3 (N-TRIPLE), .nt (N-TRIPLE)"
        );
        System.out.println(pad + "ℹ️ Using serialization language: " + serializationLanguage);
    }

    private static void validatePaths(String arg1, String arg2, String arg3) {
        Scanner scanner = new Scanner(System.in);
        boolean valid = false;

        while (!valid) {
            System.out.println(
                    "ℹ️The program has interpreted the following paths:" +
                            "\n\t1. RDF schema file: " + schemaPath +
                            "\n\t2. SPARQL construct query: " + queryPath +
                            "\n\t3. Output results file: " + outputPath +
                            "\n\t➡️ Is this correct? (y/n)"
            );

            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("y")) {
                System.out.println("\tYou answered 'yes', proceeding program execution...");
                valid = true;
            } else if (answer.equalsIgnoreCase("n")) {
                System.out.println("\tYou answered 'no', removing absolute paths...");
                schemaPath = arg1;
                queryPath = arg2;
                outputPath = arg3;
            } else {
                System.out.println("\tInput not recognized, change program arguments and try again...");
                System.exit(1);
            }
        }
    }

    private static boolean isNotValidFileExtension(String fileName) {
        List<String> allowedFileExtensions = Arrays.asList(".rdf", ".ttl", ".n3", ".nt");
        return allowedFileExtensions.stream().noneMatch(fileName::endsWith);
    }

    private static void testListStatements(String name, List<String> statements) {
        if (statements.isEmpty()) {
            System.out.println("🚫 No " + name + " RDF model statements found.");
            return;
        }
        System.out.println("✅ " + name +" RDF model statements:");
        for (String statement : statements) {
            System.out.println("\t" + statement);
        }
    }

    private static List<String> testReadRDFFile(String path) throws IOException {
        List<String> statements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                line = line.replaceAll(" {2,}", " "); // remove unnecessary spaces

                if (line.isEmpty() || line.startsWith("#")) continue;

                stringBuilder.append(line).append(" ");
                if (line.endsWith(".")) { // statement done
                    statements.add(stringBuilder.toString());
                    stringBuilder.setLength(0); // reset for the next statement
                }
            }
            // the turtle file has statements across multiple lines, add remaining text
            if (!stringBuilder.isEmpty()) statements.add(stringBuilder.toString().trim());
        }
        return statements;
    }
}
