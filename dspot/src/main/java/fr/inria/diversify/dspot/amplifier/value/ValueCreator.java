package fr.inria.diversify.dspot.amplifier.value;

import fr.inria.diversify.utils.AmplificationChecker;
import fr.inria.diversify.utils.AmplificationHelper;
import spoon.SpoonException;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.SpoonClassNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;


/**
 * User: Simon
 * Date: 07/01/16
 * Time: 10:40
 */
public class ValueCreator {

    private static final int MAX_ARRAY_SIZE = 5;
    public static int count = 0;

    @Deprecated
    public static CtLocalVariable createRandomLocalVar(CtTypeReference type) {
        return ValueCreator.createRandomLocalVar(type, "vc");
    }

    public static CtLocalVariable createRandomLocalVar(CtTypeReference type, String prefixName) {
        return type.getFactory().createLocalVariable(type, "__DSPOT_" + prefixName + "_" + count++, generateRandomValue(type));
    }

    static CtExpression<?> generateRandomValue(CtTypeReference type) {
        if (AmplificationChecker.isPrimitive(type)) {
            return generatePrimitiveRandomValue(type);
        } else {
            try {
                if (AmplificationChecker.isArray(type)) {
                    return generateArray(type);
                } else if (type.getActualClass() == String.class) {
                    return type.getFactory().createLiteral(AmplificationHelper.getRandomString(20));
                } else if (type.getActualClass() == Collection.class ||
                        type.getActualClass() == List.class ||
                        type.getSuperInterfaces().contains(type.getFactory().Type().get(List.class).getReference())) {
                    return CollectionCreator.generateCollection(type, "List", List.class);
                } else if (type.getActualClass() == Set.class) {
                    return CollectionCreator.generateCollection(type, "Set", Set.class);
                } else if (type.getActualClass() == Map.class) {
                    return CollectionCreator.generateCollection(type, "Map", Map.class);
                }
            } catch (SpoonException exception) {
                // couldn't load the definition of the class, it may be a client class
                return ConstructorCreator.generateConstructionOf(type);
            }
        }
        return ConstructorCreator.generateConstructionOf(type);
//		throw new RuntimeException();
    }

    @Deprecated
    public static CtLocalVariable generateNullValue(CtTypeReference type) {
        Factory factory = type.getFactory();
        final CtLiteral<?> defaultExpression = factory.createLiteral(null);
        defaultExpression.addTypeCast(type);
        return factory.Code().createLocalVariable(type, "vc_" + count++, defaultExpression);
    }

    private static CtExpression generateArray(CtTypeReference type) {
        CtArrayTypeReference arrayType = (CtArrayTypeReference) type;
        CtTypeReference typeComponent = arrayType.getComponentType();
        CtNewArray<?> newArray = type.getFactory().createNewArray();
        final int size = AmplificationHelper.getRandom().nextInt(MAX_ARRAY_SIZE);
        newArray.setType(arrayType);
        if (size == 0) {
            newArray.setDimensionExpressions(Collections.singletonList(type.getFactory().createLiteral(size)));
        } else if (ValueCreatorHelper.canGenerateAValueForType(typeComponent)){
            IntStream.range(0, size)
                    .mapToObj(i -> generateRandomValue(typeComponent))
                    .forEach(newArray::addElement);
        }
        return newArray;
    }


    private static CtExpression<?> generatePrimitiveRandomValue(CtTypeReference type) {
        if (type.getActualClass() == Boolean.class || type.getActualClass() == boolean.class) {
            return type.getFactory().createLiteral(AmplificationHelper.getRandom().nextBoolean());
        }
        if (type.getActualClass() == Character.class || type.getActualClass() == char.class) {
            return type.getFactory().createLiteral(AmplificationHelper.getRandomChar());
        }
        if (type.getActualClass() == Byte.class || type.getActualClass() == byte.class) {
            return type.getFactory().createLiteral((byte) AmplificationHelper.getRandom().nextInt());
        }
        if (type.getActualClass() == Short.class || type.getActualClass() == short.class) {
            return type.getFactory().createLiteral((short) AmplificationHelper.getRandom().nextInt());
        }
        if (type.getActualClass() == Integer.class || type.getActualClass() == int.class) {
            return type.getFactory().createLiteral((AmplificationHelper.getRandom().nextInt()));
        }
        if (type.getActualClass() == Long.class || type.getActualClass() == long.class) {
            return type.getFactory().createLiteral((long) AmplificationHelper.getRandom().nextInt());
        }
        if (type.getActualClass() == Float.class || type.getActualClass() == float.class) {
            return type.getFactory().createLiteral((float) AmplificationHelper.getRandom().nextDouble());
        }
        if (type.getActualClass() == Double.class || type.getActualClass() == double.class) {
            return type.getFactory().createLiteral(AmplificationHelper.getRandom().nextDouble());
        }
        throw new RuntimeException();
    }
}
