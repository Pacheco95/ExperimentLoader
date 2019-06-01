package br.ufop.decom;

import br.ufop.decom.data.Experiment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExperimentLoader {
  public static void main(String[] args) throws IOException {
    InputStream is = args.length == 0 ? System.in : Files.newInputStream(Paths.get(args[0]));
    Yaml yaml = new Yaml();
    Experiment experiment = yaml.loadAs(is, Experiment.class);
    Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    System.out.println(gson.toJson(experiment));
  }
}
