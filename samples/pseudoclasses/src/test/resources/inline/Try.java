import wn.pseudoclasses.Pseudo;
import wn.pseudoclasses.Wrapper;

public class Try {

    public static void main(String[] args) {
        MyResource mr = (MyResource) new Resource("r0");
        try (Resource r1 = new Resource("r1");
             Resource r2 = ((MyResource) new Resource("r2")).get();
             Resource r0 = mr.get();
             Resource r3 = new Resource("r3")) {
            System.out.println("block");
        }
        System.out.println("done");
    }


    static class Resource implements AutoCloseable {

        final String id;

        Resource(String id) {
            this.id = id;
            System.out.println(id + " created");
        }

        @Override
        public void close() {
            System.out.println(id + " closed");
        }
    }


    @Pseudo
    static final class MyResource extends Wrapper<Resource> {

        Resource get() {
            return value;
        }
    }
}