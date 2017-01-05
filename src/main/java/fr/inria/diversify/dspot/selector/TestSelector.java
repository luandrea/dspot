package fr.inria.diversify.dspot.selector;

import spoon.reflect.declaration.CtMethod;

import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/5/17
 */
public interface TestSelector {

    void init();

    List<CtMethod> selectToAmplify(List<CtMethod> testsToBeAmplified);

    List<CtMethod> selectToKeep(List<CtMethod> amplifiedTestToBeKept);

    void update();

}