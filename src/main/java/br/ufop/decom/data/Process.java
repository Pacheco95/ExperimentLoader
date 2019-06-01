package br.ufop.decom.data;

import lombok.Data;

import java.util.HashSet;

@Data
public class Process {
  private String id;
  private String command;
  private HashSet<String> in;
  private HashSet<String> out;
  private String log;
}
