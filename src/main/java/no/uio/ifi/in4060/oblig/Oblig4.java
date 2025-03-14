package no.uio.ifi.in4060.oblig;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: we use non-unique name assumption in semantic web, so task 2.9 IS ENTAILED -> CHANGE IT!!!
// TODO: write down in your notes the definitions and differences of soundness and completeness (it will be on the exam)
// entailment and inference are two different things

public class Oblig4 {
    private static Query query;
    private static Model resultModel;
    private static InfModel combinedModel;
    private static String serializationLanguage;
    private static final StringBuilder stringBuilder = new StringBuilder();

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException(
                "🚫 Illegal arguments: please provide:\n1: path to the RDF file\n2: path to the SPARQL construct query\n3: output file name"
        );

        String arg1 = args[0]; // path to the file your have written in the first exercise, RDFS modelling
        String arg2 = args[1]; // path to your SPARQL construct query
        String arg3 = args[2]; // file name to where the results of the SPARQL query shall be written
        System.out.println("ℹ️ Running program with arguments: " + arg1 + ", " + arg2 + " and " + arg3);

        String absolutePath = System.getProperty("user.dir") + "/src/main/java/no/uio/ifi/in4060/oblig/";
        String dataPath = "https://www.uio.no/studier/emner/matnat/ifi/IN3060/v23/obliger/simpsons.ttl";
        String schemaPath = absolutePath + arg1;
        String queryPath = absolutePath + arg2;
        String outputPath = absolutePath + arg3;

        if (!isValidFileExtension(dataPath) || !isValidFileExtension(schemaPath)) throw new IllegalArgumentException(
                "🚫 Illegal arguments: RDF file and output file must be of type .rdf, .ttl, .n3 or .nt"
        );

        if (!queryPath.endsWith(".rq")) throw new IllegalArgumentException(
                "🚫 Illegal arguments: SPARQL construct query file must be of type .rq"
        );

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
    }

    private static void writeOutputResults(String path) {
        System.out.println("ℹ️ Writing output results...");
        try (FileOutputStream writer = new FileOutputStream(path)) {
            // redundant to set serialization language again for the arguments I use (both are turtle),
            // but setting it again just in case the teacher changes the arguments
            setSerializationLanguage(path);

            resultModel.write(writer, serializationLanguage);
            System.out.println("✅ Success!");
        } catch (IOException e) {
            throw new Error("🚫 Error writing output query results to file: " + e.getMessage());
        }
    }

    private static void executeQuery() {
        System.out.println("ℹ️ Executing query...");
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, combinedModel)) {
            resultModel = queryExecution.execConstruct();
            System.out.println("✅ Success!");
        } catch (QueryParseException e) {
            throw new Error("🚫 Error executing query: " + e.getMessage());
        }
    }

    private static void initQuery(String path) {
        System.out.println("ℹ️ Initiating query...");
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
        setSerializationLanguage(uri);

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
        System.out.println("ℹ️ Initiating inferred model combined by schema and data model...");
        try {
            Model schema = getModel(schemeURI);
            Model data = getModel(dataURI);

            combinedModel = ModelFactory.createRDFSModel(schema, data);
            System.out.println("✅ Success!");
        } catch (FileNotFoundException e) {
            throw new Error("🚫 Error reading RDF file: " + e.getMessage());
        }
    }

    private static void setSerializationLanguage(String uri) {
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
        System.out.println("ℹ️ Using serialization language: " + serializationLanguage);
    }

    /**
     * Checks if the provided file name has a valid RDF serialization format extension.
     * @param fileName The file name to check.
     * @return True if the file extension is valid, false otherwise.
     */
    private static boolean isValidFileExtension(String fileName) {
        List<String> allowedFileExtensions = Arrays.asList(".rdf", ".ttl", ".n3", ".nt");
        return allowedFileExtensions.stream().anyMatch(fileName::endsWith);
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
