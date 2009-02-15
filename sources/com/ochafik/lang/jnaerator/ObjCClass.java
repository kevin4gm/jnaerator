/*
	Copyright (c) 2009 Olivier Chafik, All Rights Reserved
	
	This file is part of JNAerator (http://jnaerator.googlecode.com/).
	
	JNAerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	JNAerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with JNAerator.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ochafik.lang.jnaerator;

import static com.ochafik.util.string.StringUtils.*;
import static com.ochafik.lang.SyntaxUtils.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ochafik.lang.grammar.objcpp.Declaration;
import com.ochafik.lang.grammar.objcpp.Expression;
import com.ochafik.lang.grammar.objcpp.Function;
import com.ochafik.lang.grammar.objcpp.PrintScanner;
import com.ochafik.lang.grammar.objcpp.Scanner;
import com.ochafik.lang.grammar.objcpp.StoredDeclarations;
import com.ochafik.lang.grammar.objcpp.Struct;
import com.ochafik.lang.grammar.objcpp.TypeRef;
import com.ochafik.lang.grammar.objcpp.VariableStorage;
import com.ochafik.lang.grammar.objcpp.VariablesDeclaration;
import com.ochafik.lang.grammar.objcpp.Declaration.Modifier;
import com.ochafik.lang.grammar.objcpp.Expression.Constant;
import com.ochafik.lang.grammar.objcpp.Expression.MemberRefStyle;
import com.ochafik.lang.grammar.objcpp.TypeRef.FunctionSignature;
import com.ochafik.util.CompoundCollection;
import com.ochafik.util.string.StringUtils;

class ObjCClass {
	/**
	 * 
	 */
	private final Result result;

	/**
	 * @param result
	 */
	ObjCClass(Result result) {
		this.result = result;
	}
	
	static TypeRef ROCOCOA_ID_TYPEREF = new TypeRef.SimpleTypeRef("id");
	

	Struct type;
	//String javaPackage;
	List<Struct> categories = new ArrayList<Struct>(), protocols = new ArrayList<Struct>();
	
	public void generateWrapperFile() throws FileNotFoundException {
		if (type == null)
			return;
		
		String library = result.getLibrary(type);
		String javaPackage = result.javaPackageByLibrary.get(library);
		//String libraryClassName = result.getLibraryClassSimpleName(library);
		
		PrintWriter out = this.result.jnaerator.getClassSourceWriter(javaPackage + "." + type.getName());
		//this.result.javaPackages.add(javaPackage);
		
		out.println("package " + javaPackage + ";");
		for (String pn : this.result.javaPackages) {
			if (this.result.javaPackages.equals(javaPackage))
				continue;
			out.println("import " + pn + ".*;");
		}
		out.println("import org.rococoa.ID;");
		out.println(toRococoaHeaderDOMWithCategories());
		//out.println(toRococoaHeaderWithCategories());
		out.close();
	}

	/*
	@Deprecated
	private String toRococoaHeaderWithCategories() {
		StringBuilder s = new StringBuilder();
		if (type.getCategoryName() != null)
			s.append("/// @Protocol");
		
		List<String> extensions = new ArrayList<String>(), missingExtensions = new ArrayList<String>();
		
		String superType = null;
		if (type.getParents().isEmpty()) {
			if (!type.getName().equals("NSObject"))
				superType = "NSObject";
		} else
			superType = type.getParents().iterator().next();
			
		if (!result.objCClasses.containsKey(superType)) {
			missingExtensions.add(superType);
			if (!type.getName().equals("NSObject"))
				superType = "NSObject";
		}
		if (!extensions.contains(superType) && superType != null)
			extensions.add(superType);
		
		//extensions.add(superType == null ? "NSObject" : superType);
		for (String prot : type.getProtocols()) {
			if (prot.equals(type.getName()))
				continue;
			List<String> c = result.objCClasses.containsKey(prot) ? extensions : missingExtensions;
			if (!c.contains(prot))
				c.add(prot);
		}
		
		List<String> otherComments = new ArrayList<String>();
		otherComments.add(result.jnaerator.getFileCommentContent(type));
		
		for (Struct ss : protocols)
			if (ss.getCommentBefore() != null) {
				otherComments.add("");
				otherComments.add("Imported " + ss.getCategoryName() + " " + result.jnaerator.getFileCommentContent(ss));
				otherComments.add(ss.getCommentBefore());
			}
		
		for (Struct ss : categories)
			if (ss.getCommentBefore() != null) {
				otherComments.add("");
				otherComments.add("Imported " + ss.getCategoryName() + " " + result.jnaerator.getFileCommentContent(ss));
				otherComments.add(ss.getCommentBefore());
			}
		
		s.append("\n");
		s.append(type.formatComments("", true, otherComments.toArray(new String[0])));
		s.append("\n");
		s.append("public interface " + type.getName() + 
				(extensions.isEmpty() ? "" : " extends " + implode(extensions, ", ")) + 
				(missingExtensions.isEmpty() ? "" : " /*, " + implode(missingExtensions, ", ") + "*" + "/") +  " {\n");
		
			PrintScanner callbackScanner = new PrintScanner("") {
				Set<String> signatures = new TreeSet<String>();
				@Override
				public void visitFunctionSignature(FunctionSignature functionSignature) {
					super.visitFunctionSignature(functionSignature);
					result.jnaerator.outputCallback(result, out, functionSignature, null, signatures, "\t");
				}
			};
			for (Struct c : categories)
				c.accept(callbackScanner);
			for (Struct c : protocols)
				c.accept(callbackScanner);
			
			s.append(callbackScanner.toString());
			
			s.append("\n");
			s.append("\tpublic static final _Class CLASS = org.rococoa.Rococoa.createClass(\"" + type.getName() + "\", _Class.class);\n");
		
			MethodScanner ms = new MethodScanner(s, true);
			s.append("\tpublic interface _Class extends org.rococoa.NSClass {\n");
				type.accept(ms);
				for (Struct c : categories)
					c.accept(ms);
				for (Struct c : protocols)
					c.accept(ms);
				
			s.append("\n\t}\n");
		
			ms.setOnlyStatic(false);
			ms.existingSignatures.clear();
			type.accept(ms);
			for (Struct c : categories)
				c.accept(ms);
			for (Struct c : protocols)
				c.accept(ms);
		
		s.append("\n}\n");
		return s.toString();
	}*/
	
	
	private Struct toRococoaHeaderDOMWithCategories() {
//		StringBuilder s = new StringBuilder();
//		if (type.getCategoryName() != null)
//			s.append("/// @Protocol");
		
		List<String> extensions = new ArrayList<String>(), missingExtensions = new ArrayList<String>();
		
		String superType = null;
		if (type.getParents().isEmpty()) {
			if (!type.getName().equals("NSObject"))
				superType = "NSObject";
		} else
			superType = type.getParents().iterator().next();
			
		if (!result.objCClasses.containsKey(superType)) {
			missingExtensions.add(superType);
			if (!type.getName().equals("NSObject"))
				superType = "NSObject";
		}
		if (!extensions.contains(superType) && superType != null)
			extensions.add(superType);
		
		//extensions.add(superType == null ? "NSObject" : superType);
		for (String prot : type.getProtocols()) {
			if (prot.equals(type.getName()))
				continue;
			List<String> c = result.objCClasses.containsKey(prot) ? extensions : missingExtensions;
			if (!c.contains(prot))
				c.add(prot);
		}
		
		List<String> otherComments = new ArrayList<String>();
		otherComments.add(result.jnaerator.getFileCommentContent(type));
		
		for (Struct ss : protocols)
			if (ss.getCommentBefore() != null) {
				otherComments.add("");
				otherComments.add("Imported " + ss.getCategoryName() + " " + result.jnaerator.getFileCommentContent(ss));
				otherComments.add(ss.getCommentBefore());
			}
		
		for (Struct ss : categories)
			if (ss.getCommentBefore() != null) {
				otherComments.add("");
				otherComments.add("Imported " + ss.getCategoryName() + " " + result.jnaerator.getFileCommentContent(ss));
				otherComments.add(ss.getCommentBefore());
			}
		
		final Struct instanceStruct = new Struct();
		instanceStruct.setType(Struct.Type.JavaInterface);
		instanceStruct.addModifier(Modifier.Public);
		instanceStruct.setName(type.getName());
		instanceStruct.setParents(extensions);
		
		instanceStruct.addToCommentBefore(otherComments);
		
		PrintScanner callbackScanner = new PrintScanner("") {
			Set<String> signatures = new TreeSet<String>();
			@Override
			public void visitFunctionSignature(FunctionSignature functionSignature) {
				super.visitFunctionSignature(functionSignature);
				instanceStruct.addDeclaration(result.jnaerator.convertCallback(result, functionSignature, signatures));
			}
		};
		for (Struct c : categories)
			c.accept(callbackScanner);
		for (Struct c : protocols)
			c.accept(callbackScanner);
		
		//s.append(callbackScanner.toString());
		
		//s.append("\n");
		
		//Struct classStruct = new Struct();
		//classStruct.setName(superType)
		StoredDeclarations classHolder = new VariablesDeclaration();
		classHolder.setValueType(new TypeRef.SimpleTypeRef("_Class"));
		Expression.FunctionCall call = new Expression.FunctionCall(new Expression.TypeRefExpression(new TypeRef.SimpleTypeRef("org.rococoa.Rococoa")), "createClass", MemberRefStyle.Dot);
		call.addArgument(new Expression.Constant(Constant.Type.String, type.getName()));
		call.addArgument(new Expression.FieldRef(new Expression.TypeRefExpression(new TypeRef.SimpleTypeRef("_Class")), "class", MemberRefStyle.Dot));
		classHolder.addVariableStorage(new VariableStorage("CLASS", call));
		
		instanceStruct.addDeclaration(classHolder);
		//s.append("\tpublic static final _Class CLASS = org.rococoa.Rococoa.createClass(\"" + type.getName() + "\", _Class.class);\n");
	
		Struct classStruct = new Struct();
		classStruct.setName("_Class");
		classStruct.setType(Struct.Type.JavaInterface);
		classStruct.addParent("org.rococoa.NSClass");
		classStruct.addModifier(Modifier.Public);
		
		instanceStruct.addDeclaration(classStruct);
		
		CompoundCollection<Declaration> declarations = new CompoundCollection<Declaration>();
		declarations.addComponent(type.getDeclarations());
		for (Struct c : categories)
			declarations.addComponent(c.getDeclarations());
		for (Struct c : protocols)
			declarations.addComponent(c.getDeclarations());
	
		Set<String> signatures = new HashSet<String>();
		
		for (Declaration d : declarations) {
			if (d instanceof Function) {
				Function f = (Function)d;//as(d, Function.class);
				List<Declaration> conv = result.jnaerator.convertFunction(result, f, signatures, false);
				if (f.getModifiers().contains(Modifier.Static)) {
					classStruct.addDeclarations(conv);
				} else {
					instanceStruct.addDeclarations(conv);
				}
			}
		}
		
		return instanceStruct;
	}
}