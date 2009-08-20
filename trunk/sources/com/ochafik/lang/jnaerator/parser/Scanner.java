/*
	Copyright (c) 2009 Olivier Chafik, All Rights Reserved
	
	This file is part of JNAerator (http://jnaerator.googlecode.com/).
	
	JNAerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	JNAerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with JNAerator.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ochafik.lang.jnaerator.parser;

import java.util.ArrayList;
import java.util.Collection;

import com.ochafik.lang.jnaerator.parser.Declarator.ArrayDeclarator;
import com.ochafik.lang.jnaerator.parser.Declarator.DirectDeclarator;
import com.ochafik.lang.jnaerator.parser.Declarator.FunctionDeclarator;
import com.ochafik.lang.jnaerator.parser.Declarator.PointerDeclarator;
import com.ochafik.lang.jnaerator.parser.Declarator.TargettedDeclarator;
import com.ochafik.lang.jnaerator.parser.Enum.EnumItem;
import com.ochafik.lang.jnaerator.parser.Expression.ArrayAccess;
import com.ochafik.lang.jnaerator.parser.Expression.AssignmentOp;
import com.ochafik.lang.jnaerator.parser.Expression.BinaryOp;
import com.ochafik.lang.jnaerator.parser.Expression.Cast;
import com.ochafik.lang.jnaerator.parser.Expression.ConditionalExpression;
import com.ochafik.lang.jnaerator.parser.Expression.Constant;
import com.ochafik.lang.jnaerator.parser.Expression.EmptyArraySize;
import com.ochafik.lang.jnaerator.parser.Expression.ExpressionSequence;
import com.ochafik.lang.jnaerator.parser.Expression.FunctionCall;
import com.ochafik.lang.jnaerator.parser.Expression.MemberRef;
import com.ochafik.lang.jnaerator.parser.Expression.New;
import com.ochafik.lang.jnaerator.parser.Expression.NewArray;
import com.ochafik.lang.jnaerator.parser.Expression.NullExpression;
import com.ochafik.lang.jnaerator.parser.Expression.OpaqueExpression;
import com.ochafik.lang.jnaerator.parser.Expression.TypeRefExpression;
import com.ochafik.lang.jnaerator.parser.Expression.UnaryOp;
import com.ochafik.lang.jnaerator.parser.Expression.VariableRef;
import com.ochafik.lang.jnaerator.parser.Identifier.QualifiedIdentifier;
import com.ochafik.lang.jnaerator.parser.Identifier.SimpleIdentifier;
import com.ochafik.lang.jnaerator.parser.Statement.Block;
import com.ochafik.lang.jnaerator.parser.Statement.DeclarationStatement;
import com.ochafik.lang.jnaerator.parser.Statement.ExpressionStatement;
import com.ochafik.lang.jnaerator.parser.Statement.If;
import com.ochafik.lang.jnaerator.parser.Statement.Return;
import com.ochafik.lang.jnaerator.parser.Statement.Throw;
import com.ochafik.lang.jnaerator.parser.StoredDeclarations.TypeDef;
import com.ochafik.lang.jnaerator.parser.TypeRef.ArrayRef;
import com.ochafik.lang.jnaerator.parser.TypeRef.FunctionSignature;
import com.ochafik.lang.jnaerator.parser.TypeRef.Pointer;
import com.ochafik.lang.jnaerator.parser.TypeRef.Primitive;
import com.ochafik.lang.jnaerator.parser.TypeRef.SimpleTypeRef;
import com.ochafik.lang.jnaerator.parser.TypeRef.TaggedTypeRef;
import com.ochafik.lang.jnaerator.parser.TypeRef.TargettedTypeRef;
import com.ochafik.util.listenable.Pair;

public class Scanner implements Visitor {

	public void visitArg(Arg arg) {
		visitDeclaration(arg);
		
		visit(arg.getDefaultValue());
		
		visit(arg.getDeclarator());
	}

	public void visitConstant(Constant constant) {
		visitExpression(constant);
	}

	protected void visitExpression(Expression expression) {
		visitElement(expression);
	}

	public void visitEnum(Enum enum1) {
		visitTaggedTypeRef(enum1);
		for (EnumItem item : copy(enum1.getItems()))
			visit(item);
	}

	public void visitFunction(Function function) {
		visitDeclaration(function);
		for (Arg arg : copy(function.getArgs()))
			visit(arg);
		
		for (FunctionCall fc : function.getInitializers())
			visit(fc);
		
		visit(function.getBody());
		visit(function.getName());
	}

	public void visitFunctionPointerDeclaration(FunctionPointerDeclaration f) {
		visitDeclaration(f);
		visit(f.getDefaultValue());
	}
	
	protected void visitDeclaration(Declaration d) {
		visitModifiableElement(d);
		
		visit(d.getValueType());		
	}

	protected void visitElement(Element d) {
		
	}

	public void visitStruct(Struct struct) {
		if (struct.getType() != null) {
			switch (struct.getType()) {
			case CPPClass:
				visitCPPClass(struct);
				break;
			case CStruct:
				visitCStruct(struct);
				break;
			case ObjCClass:
				visitObjCClass(struct);
				break;
			case ObjCProtocol:
				visitObjCProtocol(struct);
				break;
			case JavaClass:
				visitJavaClass(struct);
				break;
			case JavaInterface:
				visitJavaInterface(struct);
				break;
			default:
				doVisitStruct(struct);
			}
		} else {
			doVisitStruct(struct);
		}
		
	}

	public void visitJavaClass(Struct struct) {
		doVisitStruct(struct);
	}

	public void visitJavaInterface(Struct struct) {
		doVisitStruct(struct);
	}

	protected void visitStoredDeclarations(StoredDeclarations d) {
		visitDeclaration(d);
		for (Declarator s : copy(d.getDeclarators()))
			visit(s);
	}

	protected void visitCPPClass(Struct struct) {
		doVisitStruct(struct);
	}

	protected void doVisitStruct(Struct struct) {
		visitTaggedTypeRef(struct);
		for (Declaration m : copy(struct.getDeclarations()))
			visit(m);
		for (Identifier i : struct.getProtocols())
			visit(i);
		for (Identifier i : struct.getParents())
			visit(i);
	}

	protected void visitCStruct(Struct struct) {
		doVisitStruct(struct);
	}

	protected void visitObjCClass(Struct struct) {
		doVisitStruct(struct);
	}

	protected void visitObjCProtocol(Struct struct) {
		doVisitStruct(struct);
	}

	public void visitTypeDef(TypeDef typeDef) {
		visitStoredDeclarations(typeDef);
	}

	public void visitArray(ArrayRef array) {
		visitTargettedTypeRef(array);
		for (Expression x : copy(array.getDimensions()))
			visit(x);
	}

	protected void visitTypeRef(TypeRef array) {
		visitModifiableElement(array);
	}

	protected void visitTargettedTypeRef(TargettedTypeRef targettedTypeRef) {
		visitTypeRef(targettedTypeRef);
		visit(targettedTypeRef.getTarget());
	}

	public void visitFunctionSignature(FunctionSignature functionSignature) {
		visitTypeRef(functionSignature);
		if (functionSignature != null)
			visit(functionSignature.getFunction());
	}

	public void visitPointer(Pointer pointer) {
		visitTargettedTypeRef(pointer);
	}

	public void visitPrimitive(Primitive primitive) {
		visitSimpleTypeRef(primitive);
	}

	public void visitSimpleTypeRef(SimpleTypeRef simpleTypeRef) {
		visitTypeRef(simpleTypeRef);

		visit(simpleTypeRef.getName());
	}

	static <T> Collection<T> copy(Collection<T> col) {
		return new ArrayList<T>(col);
	}
	public void visitSourceFile(SourceFile header) {
		visitElement(header);
		for (Declaration d : copy(header.getDeclarations()))
			visit(d);
	}

	public void visitEnumItem(EnumItem enumItem) {
		visitElement(enumItem);
		visit(enumItem.getValue());
	}

	public void visitUnaryOp(UnaryOp unaryOp) {
		visitExpression(unaryOp);
		visit(unaryOp.getOperand());
	}

	public void visitVariableRef(VariableRef variableRef) {
		visitExpression(variableRef);
		visit(variableRef.getName());
	}

	public void visitBinaryOp(BinaryOp binaryOp) {
		visitExpression(binaryOp);
		visit(binaryOp.getFirstOperand());
		visit(binaryOp.getSecondOperand());
	}

	public void visitFunctionCall(FunctionCall functionCall) {
		visitMemberRef(functionCall);
		visit(functionCall.getTarget());
		visit(functionCall.getFunction());
		
		for (Pair<String, Expression> x : copy(functionCall.getArguments()))
			if (x != null)
				visit(x.getSecond());
	}

	/*public void visitAssignment(Assignment assignment) {
		visitExpression(assignment);
		visitExpression(assignment.getTarget());
		visitExpression(assignment.getValue());
	}*/
	public void visitMemberRef(MemberRef memberRef) {
		visitExpression(memberRef);

		visit(memberRef.getTarget());
		visit(memberRef.getName());
		
	}

	public void visitCast(Cast cast) {
		visitExpression(cast);
		visit(cast.getType());
		visit(cast.getTarget());
	}
	
	public void visitDeclarator(Declarator declarator) {
		visitModifiableElement(declarator);
		
		visit(declarator.getDefaultValue());
	}

	public void visitVariablesDeclaration(VariablesDeclaration v) {
		visitDeclaration(v);
		for (Declarator vs : copy(v.getDeclarators()))
			visit(vs);
	}

	public void visitTaggedTypeRefDeclaration(TaggedTypeRefDeclaration taggedTypeRefDeclaration) {
		visitDeclaration(taggedTypeRefDeclaration);
		visit(taggedTypeRefDeclaration.getTaggedTypeRef());
	}

	public void visitEmptyArraySize(EmptyArraySize emptyArraySize) {
		visitExpression(emptyArraySize);
	}

	public void visitDefine(Define define) {
		visitDeclaration(define);
		visit(define.getValue());
	}

	public void visitTypeRefExpression(TypeRefExpression typeRefExpression) {
		visitExpression(typeRefExpression);
		visit(typeRefExpression.getType());
	}

	public void visitNew(New new1) {
		visitExpression(new1);
		visit(new1.getType());
		
		visit(new1.getConstruction());
	}

	public void visitAnnotation(Annotation annotation) {
		visitElement(annotation);
		for (Expression x : annotation.getArguments())
			visit(x);
	}

	public void visitEmptyDeclaration(EmptyDeclaration emptyDeclaration) {
		visitDeclaration(emptyDeclaration);
	}

	public void visitNewArray(NewArray newArray) {
		visitExpression(newArray);
		visit(newArray.getType());
		for (Expression x : newArray.getDimensions())
			visit(x);
	}

	public void visitArrayDeclarator(ArrayDeclarator arrayDeclarator) {
		visitTargettedDeclarator(arrayDeclarator);
		for (Expression x : arrayDeclarator.getDimensions())
			visit(x);
	}

	public void visitDirectDeclarator(DirectDeclarator directDeclarator) {
		visitDeclarator(directDeclarator);
	}

	public void visitFunctionDeclarator(FunctionDeclarator functionDeclarator) {
		visitTargettedDeclarator(functionDeclarator);
		for (Arg arg : functionDeclarator.getArgs())
			visit(arg);
	}

	public void visitPointerDeclarator(PointerDeclarator pointerDeclarator) {
		visitTargettedDeclarator(pointerDeclarator);
	}

	private void visitTargettedDeclarator(TargettedDeclarator targettedDeclarator) {
		visitDeclarator(targettedDeclarator);
		visit(targettedDeclarator.getTarget());
	}

	public void visitModifiableElement(ModifiableElement modifiableElement) {
		visitElement(modifiableElement);
		for (Annotation a : modifiableElement.getAnnotations())
			visit(a);
	}

	public void visitTaggedTypeRef(TaggedTypeRef taggedTypeRef) {
		visitTypeRef(taggedTypeRef);
		visit(taggedTypeRef.getTag());
		visit(taggedTypeRef.getOriginalTag());
		
	}

	public void visitBlock(Block block) {
		visitStatement(block);
		for (Statement x : copy(block.getStatements()))
			visit(x);
	}

	public void visitExpressionStatement(ExpressionStatement expressionStatement) {
		visitStatement(expressionStatement);
		visit(expressionStatement.getExpression());
	}

	public void visitStatement(Statement statement) {
		visitElement(statement);
	}

	public void visitIf(If if1) {
		visitStatement(if1);
		visit(if1.getCondition());
		visit(if1.getThenBranch());
		visit(if1.getElseBranch());
	}

	public void visitNullExpression(NullExpression nullExpression) {
		visitExpression(nullExpression);
	}

	public void visitReturn(Return return1) {
		visitStatement(return1);
		visit(return1.getValue());
	}

	public void visitExternDeclarations(ExternDeclarations externDeclarations) {
		visitDeclaration(externDeclarations);
		for (Declaration d : new ArrayList<Declaration>(externDeclarations.getDeclarations()))
			visit(d);
	}

	public void visitOpaqueExpression(OpaqueExpression opaqueExpression) {
		visitExpression(opaqueExpression);
	}

	public void visitArrayAccess(ArrayAccess arrayAccess) {
		visitExpression(arrayAccess);
		visit(arrayAccess.getTarget());
		visit(arrayAccess.getIndex());
	}

	public void visitAssignmentOp(AssignmentOp assignment) {
		visitExpression(assignment);
		visit(assignment.getTarget());
		
		visit(assignment.getValue());
	}

	
	public void visitConditionalExpression(
			ConditionalExpression conditionalExpression) {
		visitExpression(conditionalExpression);
		visit(conditionalExpression.getTest());
		visit(conditionalExpression.getThenValue());
		visit(conditionalExpression.getElseValue());
		
	}

	
	public void visitExpressionSequence(ExpressionSequence expressionSequence) {
		visitExpression(expressionSequence);
		for (Expression x : expressionSequence.getSequence())
			visit(x);
	}

	
	public void visitSimpleIdentifier(SimpleIdentifier simpleIdentifier) {
		visitIdentifier(simpleIdentifier);
		for (Expression x : simpleIdentifier.getTemplateArguments())
			visit(x);
	}

	public void visitIdentifier(Identifier identifier) {
		visitElement(identifier);
	}

	public void visitQualifiedIdentifier(QualifiedIdentifier qualifiedIdentifier) {
		visitIdentifier(qualifiedIdentifier);
		for (SimpleIdentifier i : qualifiedIdentifier.getIdentifiers())
			visit(i);
	}

	public void visitDeclarationStatement(
			DeclarationStatement declarationStatement) {
		visitStatement(declarationStatement);
		visit(declarationStatement.getDeclaration());
	}
	
	protected Scanner visit(Element e) {
		if (e != null)
			e.accept(this);
		return this;
	}

	public void visitThrow(Throw t) {
		visitStatement(t);
		visit(t.getExpression());
	}

	public void visitProperty(Property property) {
		visitDeclaration(property);
		visit(property.getDeclaration());
	}

	public void visitFriendDeclaration(FriendDeclaration friendDeclaration) {
		visitDeclaration(friendDeclaration);
		visit(friendDeclaration.getFriend());
	}

}
