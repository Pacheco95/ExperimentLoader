package br.ufop.decom.data;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;

@Data
public class Recipe {
  private String id;
  private HashSet<HashMap<String, HashSet>> uses;
}
