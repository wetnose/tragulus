package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import wn.tragulus.JavacUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:42 AM
 */
class WrapperPlugin implements SpecialPlugin {

    @Override
    public TypeMirror basicType(ProcessingHelper helper) {
        return helper.wrapperType;
    }


    @Override
    public boolean validate(ProcessingHelper helper, TypeElement type) {
        Trees trees = helper.getTreeUtils();
        TreePath typePath = trees.getPath(type);
        ClassTree classTree = (ClassTree) typePath.getLeaf();
        List<? extends Tree> args = ((ParameterizedTypeTree) classTree.getExtendsClause()).getTypeArguments();
        Tree arg = args.get(0);
        if (arg.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
            helper.suppressPrimTypeArg(diag -> arg == JavacUtils.getTree(diag));
        }
        return true;
    }


    @Override
    public void process(ProcessingHelper helper, List<TypeUsages> usages) {

    }
}
