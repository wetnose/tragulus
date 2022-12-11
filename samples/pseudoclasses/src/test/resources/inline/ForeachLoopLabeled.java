public class ForeachLoopLabeled {

    public static void main(String[] args) {
        lab: for (IntAnatomy0 an : new IntAnatomy0[] {(IntAnatomy0) 0, (IntAnatomy0) 1}) {
            int x = an.get();
            System.out.println(x);
            if (x != 0) break lab;
        }
    }
}