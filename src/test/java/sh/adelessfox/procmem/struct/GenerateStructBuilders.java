// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import com.squareup.javapoet.*;
import sh.adelessfox.procmem.Type;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class GenerateStructBuilders {
    private static final int MAX_FIELDS = 64;

    private static final Path DIRECTORY = Path.of("C:/Users/ShadelessFox/IdeaProjects/odradek/lib/procmem/src/main/java");
    private static final String PACKAGE_NAME = "sh.adelessfox.procmem.struct";

    private static final ClassName STRUCT_MEMBER_FIELD_NAME = ClassName.get(StructMember.Field.class);
    private static final ClassName STRUCT_TYPE_NAME = ClassName.get(StructType.class);
    private static final ClassName TYPE_SIZED_NAME = ClassName.get(Type.Sized.class);
    private static final TypeVariableName T_NAME = TypeVariableName.get("T");

    static void main() throws IOException {
        for (int arity = 0; arity < MAX_FIELDS; arity++) {
            JavaFile
                .builder(PACKAGE_NAME, makeBuilderImpl(arity, arity + 1 == MAX_FIELDS))
                .indent("    ")
                .skipJavaLangImports(true)
                .build().writeTo(DIRECTORY);
        }
    }

    private static TypeSpec makeBuilderImpl(int arity, boolean last) {
        var name = makeBuilderName(arity);
        var arguments = makeTypeArguments(arity);
        var argumentsP1 = makeTypeArguments(arity + 1);
        var type = ParameterizedTypeName.get(TYPE_SIZED_NAME, argumentsP1.getLast());

        var builder = TypeSpec.classBuilder(makeBuilderSimpleName(arity))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addTypeVariables(arguments);

        builder
            .addField(String.class, "name", Modifier.PRIVATE, Modifier.FINAL)
            .addField(StructDefinition.class, "layout", Modifier.PRIVATE, Modifier.FINAL);

        builder.addMethod(buildConstructorMethod(arguments, builder));

        if (arity == 0) {
            builder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, "name")
                .returns(name)
                .addStatement("return new $T(name, $T.empty(name))", name, StructDefinition.class)
                .build());
        }

        if (!last) {
            builder.addMethod(buildFieldMethod(makeBuilderName(arity + 1), arguments, argumentsP1, type));
        }

        builder.addMethod(buildPaddingMethod(name, arguments));

        if (arity > 0) {
            builder.addMethod(buildBuildMethod(arity, arguments));
            builder.addType(buildConstructorType(arguments));
        }

        return builder.build();
    }

    private static MethodSpec buildConstructorMethod(List<TypeVariableName> arguments, TypeSpec.Builder builder) {
        var constructor = MethodSpec.constructorBuilder()
            .addParameter(String.class, "name")
            .addParameter(StructDefinition.class, "layout");
        var body = CodeBlock.builder()
            .addStatement("this.name = name")
            .addStatement("this.layout = layout");

        for (TypeVariableName argument : arguments) {
            var fieldType = ParameterizedTypeName.get(STRUCT_MEMBER_FIELD_NAME, argument);
            var fieldName = argument.name.toLowerCase(Locale.ROOT);

            builder.addField(fieldType, fieldName, Modifier.PRIVATE, Modifier.FINAL);
            constructor.addParameter(fieldType, fieldName);
            body.addStatement("this.$L = $L", fieldName, fieldName);
        }

        return constructor.addCode(body.build()).build();
    }

    private static MethodSpec buildBuildMethod(int arity, List<TypeVariableName> arguments) {
        var params = new ArrayList<String>();
        for (TypeVariableName argument : arguments) {
            params.add("%s.read(pointer, resolved)".formatted(argument.name.toLowerCase(Locale.ROOT)));
        }

        return MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(T_NAME)
            .addParameter(makeConstructorName(arity), "constructor")
            .returns(ParameterizedTypeName.get(STRUCT_TYPE_NAME, T_NAME))
            .addStatement("return layout.build((pointer, resolved) -> constructor.create($L))", String.join(", ", params))
            .build();
    }

    private static MethodSpec buildFieldMethod(
        TypeName builderNameP1,
        List<TypeVariableName> arguments,
        List<TypeVariableName> argumentsP1,
        ParameterizedTypeName fieldType
    ) {
        var params = new ArrayList<String>();
        params.add("this.name");
        params.add("placement.layout()");
        for (TypeVariableName argument : arguments) {
            params.add(argument.name.toLowerCase(Locale.ROOT));
        }
        params.add("placement.field()");

        return MethodSpec.methodBuilder("field")
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(argumentsP1.getLast())
            .addParameter(String.class, "name")
            .addParameter(fieldType, "type")
            .returns(builderNameP1)
            .addStatement("var placement = layout.append(name, type)")
            .addStatement("return new $L($L)", builderNameP1, String.join(", ", params))
            .build();
    }

    private static MethodSpec buildPaddingMethod(
        TypeName builderName,
        List<TypeVariableName> arguments
    ) {
        var params = new ArrayList<String>();
        params.add("this.name");
        params.add("layout.padding(bytes)");
        for (TypeVariableName argument : arguments) {
            params.add(argument.name.toLowerCase(Locale.ROOT));
        }

        return MethodSpec.methodBuilder("padding")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(long.class, "bytes")
            .returns(builderName)
            .addStatement("return new $L($L)", builderName, String.join(", ", params))
            .build();
    }

    private static TypeSpec buildConstructorType(List<TypeVariableName> arguments) {
        var createMethod = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(T_NAME);
        for (var argument : arguments) {
            createMethod.addParameter(argument, argument.name.toLowerCase(Locale.ROOT));
        }

        return TypeSpec.interfaceBuilder("Constructor")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(FunctionalInterface.class)
            .addTypeVariables(arguments)
            .addTypeVariable(T_NAME)
            .addMethod(createMethod.build())
            .build();
    }

    private static TypeName makeBuilderName(int arity) {
        var name = makeBuilderSimpleName(arity);
        var arguments = makeTypeArguments(arity);
        if (arguments.isEmpty()) {
            return name;
        }
        return ParameterizedTypeName.get(name, arguments.toArray(TypeName[]::new));
    }

    private static ClassName makeBuilderSimpleName(int arity) {
        return ClassName.get("", "StructBuilder" + arity);
    }

    private static TypeName makeConstructorName(int arity) {
        var name = makeBuilderSimpleName(arity).nestedClass("Constructor");
        var arguments = makeTypeArgumentsPlusT(arity);
        return ParameterizedTypeName.get(name, arguments.toArray(TypeName[]::new));
    }

    private static ArrayList<TypeVariableName> makeTypeArgumentsPlusT(int arity) {
        var arguments = new ArrayList<TypeVariableName>(arity + 1);
        arguments.addAll(makeTypeArguments(arity));
        arguments.add(T_NAME);
        return arguments;
    }

    private static List<TypeVariableName> makeTypeArguments(int arity) {
        return IntStream.range(0, arity)
            .mapToObj(i -> TypeVariableName.get("F" + i))
            .toList();
    }
}
