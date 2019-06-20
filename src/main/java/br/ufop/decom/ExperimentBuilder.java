package br.ufop.decom;

import br.ufop.decom.data.Experiment;
import br.ufop.decom.data.Process;
import br.ufop.decom.data.Recipe;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class ExperimentBuilder {
  private static final Pattern MACRO_PATTERN = Pattern.compile("\\{([^{}]+)}");
  private final Experiment experiment;

  Set<Task> build() {
    Set<Task> tasks = new LinkedHashSet<>();
    Map<String, Set<Task>> tasksGroupedByType = new HashMap<>();
    long id = 0;
    for (Process process : experiment.getProcesses()) {
      for (Recipe recipe : experiment.getRecipes()) {
        Set<Map<String, String>> cartesianProducts = getArgumentsMapping(process, recipe);
        for (Map<String, String> cartesianProduct : cartesianProducts) {
          String command = buildCommand(process, cartesianProduct);
          Task task =
              new Task(
                  String.format("%s-%d", process.getId(), ++id), process.getId(), command, new HashSet<>());
          if (tasksGroupedByType.containsKey(task.getType())) {
            tasksGroupedByType.get(task.getType()).add(task);
          } else {
            Set<Task> tasksSet = new HashSet<>();
            tasksSet.add(task);
            tasksGroupedByType.put(task.getType(), tasksSet);
          }
          tasks.add(task);
        }
      }
    }
    updateTaskDependencies(tasks, tasksGroupedByType);
    return tasks;
  }

  private void updateTaskDependencies(Set<Task> tasks, Map<String, Set<Task>> tasksGroupedByType) {
    Map<String, Set<String>> dependsOf = getProcessesDependenciesRelation();

    // Mapeia cada tipo de processo aos IDs das dependências
    Map<String, Set<String>> compiledDependencyIDs = new HashMap<>();

    for (Map.Entry<String, Set<String>> entry : dependsOf.entrySet()) {
      Set<String> dependencyIDs = new HashSet<>();
      String processType = entry.getKey();
      Set<String> dependencyTypesOfCurrentProcess = entry.getValue();
      compiledDependencyIDs.put(processType, dependencyIDs);
      for (String dependencyType : dependencyTypesOfCurrentProcess) {
        Set<Task> dependenciesByType = tasksGroupedByType.get(dependencyType);
        List<String> ids = dependenciesByType.stream().map(Task::getId).collect(Collectors.toList());
        dependencyIDs.addAll(ids);
      }
    }

    for (Task task : tasks) {
      task.deps = compiledDependencyIDs.get(task.getType());
    }
  }

  private Map<String, Set<String>> getProcessesDependenciesRelation() {
    Map<String, Set<String>> dependsOf = new HashMap<>();
    experiment.getProcesses().forEach(process -> dependsOf.put(process.getId(), new HashSet<>()));

    for (Process from : experiment.getProcesses()) {
      for (Process to : experiment.getProcesses()) {
        if (from == to) continue;
        if (!Collections.disjoint(from.getIn(), to.getOut())) {
          dependsOf.get(from.getId()).add(to.getId());
        }
      }
    }
    return dependsOf;
  }

  /**
   * @param process processos para o qual será gerado o comando
   * @param kvMap mapa que associa um argumento/valor a ser substituído no comando
   * @return o comando gerado
   */
  private String buildCommand(Process process, Map<String, String> kvMap) {
    String prefix = replaceString(process.getCommand(), kvMap);
    String suffix = replaceString(process.getLog(), kvMap);
    return String.format("%s > %s", prefix, suffix);
  }

  private Set<String> getProcessRequiredArgumentsInOrder(Process process) {
    Set<String> processRequiredArgumentsInOrder = new LinkedHashSet<>();
    Matcher matcher = MACRO_PATTERN.matcher(process.getCommand());
    while (matcher.find()) {
      processRequiredArgumentsInOrder.add(matcher.group(1));
    }
    return processRequiredArgumentsInOrder;
  }

  /**
   * Retorna um conjunto de mapas onde cada mapa representa as combinações retornadas pelo produto
   * cartesiano, porém de forma indexada.
   *
   * <p>Cada mapa tem a seguinte estrutura:
   *
   * <pre>
   *   Key: ArgumentType
   *   Value: ArgumentValue
   * </pre>
   *
   * Exemplo para um processo que receba Database e Algorithm como parâmetros:
   *
   * <pre>
   *   "Database" -> "Jester"
   *   "Algorithm" -> "UserKNN"
   * </pre>
   */
  private Set<Map<String, String>> getArgumentsMapping(Process process, Recipe recipe) {
    Set<Map<String, String>> cartesianProductMap = new HashSet<>();
    Set<String> processRequiredArgumentsInOrder = getProcessRequiredArgumentsInOrder(process);

    // Cada lista interna é referente aos valores de um argumento
    List<List<String>> allArgsWithAllValues = new ArrayList<>();

    for (String argumentType : processRequiredArgumentsInOrder) {
      List<String> argumentValues;
      if (recipe.getUses().containsKey(argumentType)) {
        argumentValues = new ArrayList<>(recipe.getUses().get(argumentType));
      } else if (experiment.getRecipeDefaults().containsKey(argumentType)) {
        argumentValues = new ArrayList<>(experiment.getRecipeDefaults().get(argumentType));
      } else {
        throw new IllegalArgumentException(
            String.format("Undefined recipe usage: \"%s\"", argumentType));
      }
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
    Matcher matcher = MACRO_PATTERN.matcher(s);
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
