package boa.functions.code.change.refactoring;

import java.util.Arrays;
import java.util.HashSet;

public enum BoaCodeElementLevel {
	UNKOWN(),
	PACKAGE_LEVEL("Change Package"),
	CLASS_LEVEL("Move Class", "Rename Class", 
			"Extract Superclass", "Extract Interface"),
	METHOD_LEVEL("Rename Method", "Inline Method", 
			"Extract Method", "Extract And Move Method", 
			"Move Method", "Pull Up Method", 
			"Push Down Method"),
	FIELD_LEVEL("Move Attribute", "Pull Up Attribute", 
			"Push Down Attribute");
	
	private HashSet<String> refactoringTypes = new HashSet<String>();
	
	private BoaCodeElementLevel(String ... refactoringType) {
		refactoringTypes.addAll(Arrays.asList(refactoringType));
	}
	
	public static BoaCodeElementLevel getCodeElementLevel(String refactoringType) {
		for (BoaCodeElementLevel cl : BoaCodeElementLevel.values()) {
			if (cl.refactoringTypes.contains(refactoringType))
				return cl;
		}
//		return UNKOWN;
		throw new IllegalArgumentException("level not known " + refactoringType);
	}
}
