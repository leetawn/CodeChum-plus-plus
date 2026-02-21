package com.exception.ccpp.FileManagement;

import com.exception.ccpp.CCJudge.QuestionFile;
import com.exception.ccpp.Crypto.AESDecrypt;
import tools.jackson.databind.*;

import java.nio.file.Path;

import static com.exception.ccpp.Common.Helpers.mergeStrings;

public class CCJsonMapper  {

    private static ObjectMapper mapper = new ObjectMapper();
    public static QuestionFile createQuestion(Path json_path, Path question_file_path) {
        JsonNode root = mapper.readTree(json_path);
        JsonNode problem = root.get("problem");
        JsonNode test_cases = problem.get("test_cases");
        JsonNode boilerplates = problem.get("boilerplates");
        JsonNode creator = problem.get("creator");

        int max_score = root.get("max_score").asInt();
        String title =  problem.get("name").asString();
        String description = problem.get("description").asString();
        JsonNode fn = creator.get("first_name");
        JsonNode ln = creator.get("last_name");
        String first_name = fn != null ? fn.asString() : "";
        String last_name = ln != null ? ln.asString() : "";
        String author = first_name + " " + last_name;

        QuestionFile qf = QuestionFile.open(question_file_path, false);
        System.out.println("Description: " + description);
        System.out.println("Title: " + title);
        qf.setDescription(description);
        qf.setTitle(title);
        if (!author.isBlank())
            qf.setAuthor(author);

        // BOILERPLATES
        for (int i = 0; i < boilerplates.size(); i++) {
            JsonNode boilerplate = boilerplates.get(i);
            String fname = boilerplate.get("file_name").asString();
            String fext = boilerplate.get("file_extension").asString();
            String code = boilerplate.get("code").asString();
            boolean is_hidden = boilerplate.get("is_hidden").asBoolean();
            boolean is_readonly = boilerplate.get("is_read_only").asBoolean();
            String filename = fname + "." + fext;
            qf.addBoilerplate(code, filename, is_hidden, is_readonly);
        }

        // TESTCASES
        for (int i = 0; i < test_cases.size(); i++) {
            JsonNode test_case = test_cases.get(i);
            JsonNode inputs_node = test_case.get("inputs");
            String output = test_case.get("output").asString();
            String[] inputs = {};
            for (int j = 0; j < inputs_node.size(); j++) {
                String in = inputs_node.get(j).get("value").asString().trim();
                inputs = mergeStrings(inputs, in.split("\\R"));
            }
            String output_decrypted = null;
            try {
                output_decrypted = AESDecrypt.decrypt(output).trim();
            } catch (Exception ignore) {
                System.err.println("Error while decrypting output");
            }
            qf.addTestcase(inputs, output_decrypted);
        }
        return qf;
    }

}
