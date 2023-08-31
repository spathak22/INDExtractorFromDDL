package ddlind.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.charset.Charset;

public class FileUtil {

    public static String writeIndsToJsonFormat(List<String> ind, Map<String, Integer> ordPos, String filePath){
        JsonObject jobParent = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (String insString: ind){
            String [] line = insString.split(Constants.Regex.SUBSET.getValue());
            String uNode = line[0].trim();
            uNode = uNode.substring(1, uNode.length()-1);
            String vNode = line[1].trim();
            vNode = vNode.substring(1, vNode.length()-1);

            JsonObject jobChild = new JsonObject();
            String [] un = uNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
            String [] vn = vNode.split(Pattern.quote(Constants.Regex.PERIOD.getValue()));
            jobChild.addProperty("leftRelation", un[0]);
            jobChild.addProperty("leftAttributeNumber", ordPos.get(uNode));
            jobChild.addProperty("rightRelation", vn[0]);
            jobChild.addProperty("rightAttributeNumber", ordPos.get(vNode));
            JsonElement element = gson.fromJson(jobChild.toString(), JsonElement.class);
            jsonArray.add(element);
        }
        jobParent.add("inds", jsonArray);
        try{
            FileWriter file = new FileWriter(filePath);
//            System.out.println(" ----------------  Json Output ------------------------");
            file.write(gson.toJson(jobParent));
            file.flush();
            file.close();
            return "Success";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Failed";
    }

    public static void writeInvalidPksToFile(List<String> invalidPks, String filePath){
        try{
            Path out = Paths.get(filePath);
            Files.write(out,invalidPks,Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
