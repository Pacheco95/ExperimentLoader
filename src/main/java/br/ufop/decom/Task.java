package br.ufop.decom;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Task {
  String id;
  String type;
  String command;
  Set<String> deps;

  @Override
  public String toString() {
    return
        String.format("- id: %s\n", id) +
        String.format("  command: \"%s\"\n", command) +
        String.format("  deps: %s\n", deps);
  }
}
