package br.ufop.decom;

import br.ufop.decom.data.Experiment;
import br.ufop.decom.data.Process;
import br.ufop.decom.data.Recipe;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ExperimentBuilder {
  private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)}");

  private final Experiment experiment;

  public Set<Task> build() {
    Set<Task> tasks = new LinkedHashSet<>();
    for (Process process : experiment.getProcesses()) {
      for (Recipe recipe : experiment.getRecipes()) {
        Set<Map<String, String>> cartesianProducts = getArgumentsMapping(process, recipe);
        for (Map<String, String> cartesianProduct : cartesianProducts) {
          String command = buildCommand(process, cartesianProduct);
          tasks.add(new Task(process.getId(), command, null));
        }
      }
    }
    return tasks;
  }

  /**
   * @param process
   * @param kvMap mapa que associa um argumento/valor a ser substituído no comando
   * @return
   */
  private String buildCommand(Process process, Map<String, String> kvMap) {
    String prefix = replaceString(process.getCommand(), kvMap);
    String suffix = replaceString(process.getLog(), kvMap);
    return String.format("%s > %s", prefix, suffix);
  }

  private Set<String> getProcessRequiredArgumentsInOrder(Process process) {
    Set<String> processRequiredArgumentsInOrder = new LinkedHashSet<>();
    Matcher matcher = PATTERN.matcher(process.getCommand());
    while (matcher.find()) {
      processRequiredArgumentsInOrder.add(matcher.group(1));
    }
    return processRequiredArgumentsInOrder;
  }

  /**
   * Retorna um conjunto de mapas onde cada mapa é a relação argumento/valor
   * */
  private Set<Map<String, String>> getArgumentsMapping(Process process, Recipe recipe) {
    Set<Map<String, String>> cartesianProductMap = new HashSet<>();
    Set<String> processRequiredArgumentsInOrder = getProcessRequiredArgumentsInOrder(process);

    // Cada lista interna é referente aos valores de um argumento
    List<List<String>> allArgsWithAllValues = new ArrayList<>();

    for (String argumentType : processRequiredArgumentsInOrder) {
      List<String> argumentValues = new ArrayList<>(recipe.getUses().get(argumentType));
      allArgsWithAllValues.add(argumentValues);
    }

    List<List<String>> cartesianProducts = cartesianProduct(allArgsWithAllValues);
    for (List<String> cartesianProduct : cartesianProducts) {
      Iterator<String> requiredArgumentsIterator = processRequiredArgumentsInOrder.iterator();
      Map<String, String> kvMap = new HashMap<>();
      for (String argumentValue : cartesianProduct) {
        kvMap.put(requiredArgumentsIterator.next(), argumentValue);
      }
      cartesianProductMap.add(kvMap);
    }

    return cartesianProductMap;
  }

  private String replaceString(String s, Map<String, String> kvMap) {
    StringBuilder builder = new StringBuilder();
    Matcher matcher = PATTERN.matcher(s);
    int start = 0;
    int end = 0;
    while (matcher.find()) {
      String newValue = kvMap.get(matcher.group(1));
      builder.append(s, start, matcher.start()).append(newValue);
      end = matcher.end();
      start = end;
    }
    builder.append(s.substring(end));
    return builder.toString();
  }


  private <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
    List<List<T>> resultLists = new ArrayList<>();
    if (lists.size() == 0) {
      resultLists.add(new ArrayList<>());
      return resultLists;
    } else {
      List<T> firstList = lists.get(0);
      List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
      for (T condition : firstList) {
        for (List<T> remainingList : remainingLists) {
          ArrayList<T> resultList = new ArrayList<>();
          resultList.add(condition);
          resultList.addAll(remainingList);
          resultLists.add(resultList);
        }
      }
    }
    return resultLists;
  }
}
