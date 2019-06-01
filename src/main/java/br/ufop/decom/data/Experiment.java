package br.ufop.decom.data;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;

@Data
public class Experiment {
  private HashSet<String> arguments;
  private HashSet<Process> processes;
  private HashSet<HashMap<String, HashSet<String>>> recipeDefaults;
  private HashSet<Recipe> recipes;
}
