package com.exception.ccpp.FileManagement;

import com.exception.ccpp.CCJudge.ActivityFile;
import com.exception.ccpp.CCJudge.CCQuestion;
import com.exception.ccpp.Crypto.AESDecrypt;
import com.kitfox.svg.A;
import tools.jackson.databind.*;

import java.nio.file.Path;

import static com.exception.ccpp.Common.Helpers.mergeStrings;

public class CCJsonMapper  {

    private static ObjectMapper mapper = new ObjectMapper();
    public static ActivityFile createActivity(Path json_path, Path question_file_path) {
        ActivityFile af = ActivityFile.open(question_file_path, false);
        JsonNode root = mapper.readTree(json_path);
        String name = root.get("name").asString();
        JsonNode questions = root.get("questions");

        af.setTitle(name);

        for (int i =0; i < questions.size(); i++) {
            CCQuestion q = createQuestion(questions.get(i));
            if (q != null)
                af.addQuestion(q);
        }

        return af;
    }


    public static CCQuestion createQuestion(JsonNode root) {
        JsonNode problem = root.get("problem");
        String problem_type = problem.get("problem_type").asString();
        if (!problem_type.equalsIgnoreCase("coding")) {
            return null;
        }
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

        CCQuestion cq = new CCQuestion();
        System.out.println("Description: " + description);
        System.out.println("Title: " + title);
        cq.setDescription(description);
        cq.setTitle(title);
        if (!author.isBlank())
            cq.setAuthor(author);

        // BOILERPLATES
        for (int i = 0; i < boilerplates.size(); i++) {
            JsonNode boilerplate = boilerplates.get(i);
            String fname = boilerplate.get("file_name").asString();
            String fext = boilerplate.get("file_extension").asString();
            String code = boilerplate.get("code").asString();
            boolean is_hidden = boilerplate.get("is_hidden").asBoolean();
            boolean is_readonly = boilerplate.get("is_read_only").asBoolean();
            String filename = fname + "." + fext;
            cq.addBoilerplate(code, filename, is_hidden, is_readonly);
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
            cq.addTestcase(inputs, output_decrypted);
        }
        return cq;
    }

}
