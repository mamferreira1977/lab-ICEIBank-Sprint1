package br.pucminas.iceibank.service;
import java.util.Arrays;
public final class ComparadorVetorial {
 private ComparadorVetorial(){}
 public static String comparar(int[] a,int[] b){
  if(a==null||b==null||a.length!=b.length||a.length==0)throw new IllegalArgumentException("Vetores invalidos");
  boolean menor=true,maior=true;
  for(int i=0;i<a.length;i++){if(a[i]>b[i])menor=false;if(b[i]>a[i])maior=false;}
  if(menor&&maior)return "IGUAIS";if(menor)return "ANTES";if(maior)return "DEPOIS";return "CONCORRENTES";
 }
}
