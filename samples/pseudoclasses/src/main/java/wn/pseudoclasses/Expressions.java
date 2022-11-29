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
class Expressions {


    enum Type {
        BOOLEAN {
            @Override
            Object evalPromoted(Tree.Kind op, Object arg) {
                return Type.evalBool(op, (boolean) arg);
            }
            @Override
            public Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
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
            Object evalPromoted(Tree.Kind op, Object arg) {
                return Type.evalInt(op, (int) arg);
            }
            @Override
            public Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                        return Type.evalInt(op, ((Number) lhs).intValue(), ((Number) rhs).intValue());
                    case LONG:
                        switch (op) {
                            case LEFT_SHIFT           :
                            case RIGHT_SHIFT          :
                            case UNSIGNED_RIGHT_SHIFT :
                                return Type.shiftLong(op, ((Number) lhs).intValue(), ((Number) rhs).longValue());
                        }
                        break;
                }
                return LONG.evalPromoted(op, lhs, rhs); //todo correct for shift operators
            }
        },

        LONG {
            @Override
            Object evalPromoted(Tree.Kind op, Object arg) {
                return Type.evalLong(op, (long) arg);
            }
            @Override
            public Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                    case LONG:
                        return Type.evalLong(op, ((Number) lhs).longValue(), ((Number) rhs).longValue());
                    default:
                        return FLOAT.evalPromoted(op, lhs, rhs);
                }
            }
        },

        FLOAT {
            @Override
            Object evalPromoted(Tree.Kind op, Object arg) {
                return Type.evalFloat(op, (float) arg);
            }
            @Override
            Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
                switch (Type.of(rhs)) {
                    case INT:
                    case LONG:
                    case FLOAT:
                        return Type.evalFloat(op, ((Number) lhs).floatValue(), ((Number) rhs).floatValue());
                    case DOUBLE:
                        return Type.evalDouble(op, ((Number) lhs).doubleValue(), ((Number) rhs).doubleValue());
                    default:
                        return null;
                }
            }
        },

        DOUBLE  {
            @Override
            Object evalPromoted(Tree.Kind op, Object arg) {
                return Type.evalDouble(op, (double) arg);
            }
            @Override
            Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
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
            Object evalPromoted(Tree.Kind op, Object arg) {
                return null;
            }
            @Override
            Object evalPromoted(Tree.Kind op, Object lhs, Object rhs) {
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


        abstract Object evalPromoted(Tree.Kind op, Object arg);
        abstract Object evalPromoted(Tree.Kind op, Object lhs, Object rhs);


        private static Object evalBool(Tree.Kind op, boolean a) {
            switch (op) {
                case LOGICAL_COMPLEMENT   : return !a;
            }
            return null;
        }


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


        private static Object evalInt(Tree.Kind op, int a) {
            switch (op) {
                case PREFIX_INCREMENT     : return a + 1;
                case PREFIX_DECREMENT     : return a - 1;
                case UNARY_PLUS           : return +a;
                case UNARY_MINUS          : return -a;
                case BITWISE_COMPLEMENT   : return ~a;
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


        private static Object evalLong(Tree.Kind op, long a) {
            switch (op) {
                case PREFIX_INCREMENT     : return a + 1;
                case PREFIX_DECREMENT     : return a - 1;
                case UNARY_PLUS           : return +a;
                case UNARY_MINUS          : return -a;
                case BITWISE_COMPLEMENT   : return ~a;
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


        private static Object shiftLong(Tree.Kind op, int l, long r) {
            switch (op) {
                case LEFT_SHIFT           : return l << r;
                case RIGHT_SHIFT          : return l >> r;
                case UNSIGNED_RIGHT_SHIFT : return l >>> r;
            }
            return null;
        }


        private static Object evalFloat(Tree.Kind op, float a) {
            switch (op) {
                case PREFIX_INCREMENT     : return a + 1;
                case PREFIX_DECREMENT     : return a - 1;
                case UNARY_PLUS           : return +a;
                case UNARY_MINUS          : return -a;
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


        private static Object evalDouble(Tree.Kind op, double a) {
            switch (op) {
                case PREFIX_INCREMENT     : return a + 1;
                case PREFIX_DECREMENT     : return a - 1;
                case UNARY_PLUS           : return +a;
                case UNARY_MINUS          : return -a;
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
        return Type.of(lhs).evalPromoted(op, lhs, rhs);
    }


    static Object eval(Tree.Kind op, Object arg) {
        return Type.of(arg).evalPromoted(op, arg);
    }
}
