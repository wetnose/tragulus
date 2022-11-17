package wn.pseudoclasses;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.lang.model.element.TypeElement;
import java.util.List;

import static wn.pseudoclasses.ProcessingHelper.Err.PRIM_TYPE_ARG;

/**
 * Alexander A. Solovioff
 * Date: 30.04.2021
 * Time: 12:42 AM
 */
class WrapperPlugin implements SpecialPlugin {


    @Override
    public boolean accept(ProcessingHelper helper, TypeElement type) {
        TypeElement baseType = helper.getBaseType(type);
        return baseType != null && baseType.asType() == helper.wrapperType;
    }


    @Override
    public boolean validate(ProcessingHelper helper, TypeElement type) {
        Trees trees = helper.getTreeUtils();
        TreePath typePath = trees.getPath(type);
        ClassTree classTree = (ClassTree) typePath.getLeaf();
        List<? extends Tree> args = ((ParameterizedTypeTree) classTree.getExtendsClause()).getTypeArguments();
        Tree arg = args.get(0);
        if (arg.getKind() == Tree.Kind.PRIMITIVE_TYPE) {
            helper.suppressDiagnostics(PRIM_TYPE_ARG, arg);
        }
        return true;
    }


    @Override
    public void process(ProcessingHelper helper, List<TypeUsages> usages) {

    }
}
