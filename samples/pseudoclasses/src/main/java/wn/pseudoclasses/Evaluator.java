package wn.pseudoclasses;

import com.sun.source.tree.Tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Alexander A. Solovioff
 * Date: 29.11.2022
 * Time: 2:48 AM
 * todo: test this
 */
class Evaluator {


    enum Type {
        BOOLEAN {
            @Override
            public Object eval(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case BOOLEAN:
                        return Type.evalBool(op, (boolean) lhs, (boolean) rhs);
                    default:
                        return null;
                }
            }
        },

        INT {
            @Override
            public Object eval(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                        return Type.evalInt(op, ((Number) lhs).intValue(), ((Number) rhs).intValue());
                    default:
                        return LONG.eval(op, lhs, rhs);

                }
            }
        },

        LONG {
            @Override
            public Object eval(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                    case LONG:
                        return Type.evalLong(op, ((Number) lhs).intValue(), ((Number) rhs).intValue());
                    default:
                        return FLOAT.eval(op, lhs, rhs);
                }
            }
        },

        FLOAT {
            @Override
            Object eval(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                    case LONG:
                    case FLOAT:
                        return Type.evalFloat(op, ((Number) lhs).floatValue(), ((Number) rhs).floatValue());
                    case DOUBLE:
                        return DOUBLE.eval(op, lhs, rhs);
                    default:
                        return null;
                }
            }
        },

        DOUBLE  {
            @Override
            Object eval(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                        return Type.evalDouble(op, ((Number) lhs).doubleValue(), ((Number) rhs).doubleValue());
                    default:
                        return null;
                }
            }
        },

        OTHER  {
            @Override
            Object eval(Tree.Kind op, Object lhs, Object rhs) {
                return null;
            }
        },

        ;


        static final Map<Class<?>, Type> map;

        static {
            HashMap<Class<?>, Type> m = new HashMap<>();
            m.put(Boolean.class   , BOOLEAN);
            m.put(Character.class , INT);
            m.put(Byte.class      , INT);
            m.put(Short.class     , INT);
            m.put(Integer.class   , INT);
            m.put(Long.class      , LONG);
            m.put(Float.class     , FLOAT);
            m.put(Double.class    , DOUBLE);
            map = Collections.unmodifiableMap(m);
        }


        static Type of(Object val) {
            Type t = map.get(val.getClass());
            return t != null ? t : OTHER;
        }


        abstract Object eval(Tree.Kind op, Object lhs, Object rhs);


        private static Object evalBool(Tree.Kind op, boolean l, boolean r) {
            switch (op) {
                case EQUAL_TO             : return l == r;
                case NOT_EQUAL_TO         : return l != r;
                case AND                  : return l & r;
                case XOR                  : return l ^ r;
                case OR                   : return l | r;
                case CONDITIONAL_AND      : return l && r;
                case CONDITIONAL_OR       : return l || r;
            }
            return null;
        }


        private static Object evalInt(Tree.Kind op, int l, int r) {
            switch (op) {
                case MULTIPLY             : return l * r;
                case DIVIDE               : return l / r;
                case REMAINDER            : return l % r;
                case PLUS                 : return l + r;
                case MINUS                : return l - r;
                case LEFT_SHIFT           : return l << r;
                case RIGHT_SHIFT          : return l >> r;
                case UNSIGNED_RIGHT_SHIFT : return l >>> r;
                case LESS_THAN            : return l < r;
                case GREATER_THAN         : return l > r;
                case LESS_THAN_EQUAL      : return l <= r;
                case GREATER_THAN_EQUAL   : return l >= r;
                case EQUAL_TO             : return l == r;
                case NOT_EQUAL_TO         : return l != r;
                case AND                  : return l & r;
                case XOR                  : return l ^ r;
                case OR                   : return l | r;
            }
            return null;
        }


        private static Object evalLong(Tree.Kind op, long l, long r) {
            switch (op) {
                case MULTIPLY             : return l * r;
                case DIVIDE               : return l / r;
                case REMAINDER            : return l % r;
                case PLUS                 : return l + r;
                case MINUS                : return l - r;
                case LEFT_SHIFT           : return l << r;
                case RIGHT_SHIFT          : return l >> r;
                case UNSIGNED_RIGHT_SHIFT : return l >>> r;
                case LESS_THAN            : return l < r;
                case GREATER_THAN         : return l > r;
                case LESS_THAN_EQUAL      : return l <= r;
                case GREATER_THAN_EQUAL   : return l >= r;
                case EQUAL_TO             : return l == r;
                case NOT_EQUAL_TO         : return l != r;
                case AND                  : return l & r;
                case XOR                  : return l ^ r;
                case OR                   : return l | r;
            }
            return null;
        }


        private static Object evalFloat(Tree.Kind op, float l, float r) {
            switch (op) {
                case MULTIPLY             : return l * r;
                case DIVIDE               : return l / r;
                case REMAINDER            : return l % r;
                case PLUS                 : return l + r;
                case MINUS                : return l - r;
                case LESS_THAN            : return l < r;
                case GREATER_THAN         : return l > r;
                case LESS_THAN_EQUAL      : return l <= r;
                case GREATER_THAN_EQUAL   : return l >= r;
                case EQUAL_TO             : return l == r;
                case NOT_EQUAL_TO         : return l != r;
            }
            return null;
        }


        private static Object evalDouble(Tree.Kind op, double l, double r) {
            switch (op) {
                case MULTIPLY             : return l * r;
                case DIVIDE               : return l / r;
                case REMAINDER            : return l % r;
                case PLUS                 : return l + r;
                case MINUS                : return l - r;
                case LESS_THAN            : return l < r;
                case GREATER_THAN         : return l > r;
                case LESS_THAN_EQUAL      : return l <= r;
                case GREATER_THAN_EQUAL   : return l >= r;
                case EQUAL_TO             : return l == r;
                case NOT_EQUAL_TO         : return l != r;
            }
            return null;
        }
    }


    static Object eval(Tree.Kind op, Object lhs, Object rhs) {
        return Type.of(lhs).eval(op, lhs, rhs);
    }
}
