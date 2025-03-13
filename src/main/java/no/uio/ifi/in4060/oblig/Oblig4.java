package no.uio.ifi.in4060.oblig;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** In this exercise you shall write a java program and a SPARQL CONSTRUCT query.
 * The SPARQL query is similar to a query you made for the previous mandatory exercise set:
 * it shall produce a FOAF file for Homer Simpson where he foaf:knows all foaf:Person-s he has a family relation to (fam:isRelativeOf).
 *
 */
public class Oblig4 {
    private static Query query;
    private static Model resultModel;
    private static InfModel combinedModel;
    private static final StringBuilder stringBuilder = new StringBuilder();

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException(
                "🚫 Missing arguments, please provide:\n1: path to the RDF file\n2: path to the SPARQL construct query\n3: output file name"
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
        // TODO: validation

        try {
            System.out.println("ℹ️ Testing...");
            List<String> familyStatements = testReadRDFFile(schemaPath);
            List<String> simpsonsStatements = testReadRDFFile(absolutePath + "simpsons.ttl");
            testListStatements("Family", familyStatements);
            testListStatements("Simpsons", simpsonsStatements);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        initCombinedModel(schemaPath, dataPath);
        initQuery(queryPath);
        executeQuery();
        writeOutputResults(outputPath);
    }

    private static void writeOutputResults(String path) {
        System.out.println("ℹ️ Writing output results...");
        try (FileOutputStream writer = new FileOutputStream(path)) {
            resultModel.write(writer, "TURTLE");
            // I have checked that output is successfully written ✅
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
            // I have logged that query is created and correct
        } catch (IOException e) {
            throw new Error("🚫 Error reading SPARQL file: " + e.getMessage());
        }
    }

    private static Model getModel(String uri) throws FileNotFoundException {
        System.out.println("\tℹ️ Getting model with uri: " + uri);
        Model model = ModelFactory.createDefaultModel();

        if (uri.startsWith("http")) {
            model.read(uri, null, "TURTLE");
        } else {
            InputStream inputStream = new FileInputStream(uri);
            model.read(inputStream, null, "TURTLE");
        }

        System.out.println("\t✅ Success!");
        // I have logged that namespaces exist
        return model;
    }

    private static void initCombinedModel(String schemeURI, String dataURI) {
        System.out.println("ℹ️ Initiating inferred model combined by schema and data model...");
        try {
            Model schema = getModel(schemeURI);
            Model data = getModel(dataURI);
            // I have logged that we get the models ✅
            combinedModel = ModelFactory.createRDFSModel(schema, data);
            System.out.println("✅ Success!");
        } catch (FileNotFoundException e) {
            throw new Error("🚫 Error reading RDF file: " + e.getMessage());
        }
        // I have logged that namespaces are correct, and I am getting resources
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
                    // add complete statement to the list, check: trim to remove any extra whitespace?
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
