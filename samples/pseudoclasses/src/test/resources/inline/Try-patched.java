
public class Try {

    public Try() {
        super();
    }

    public static void main(String[] args) {
        .Try.Resource mr = new Resource("r0");
        {
            final Resource r1 = new Resource("r1");
            .Try.Resource self0 = new Resource("r2");
            .Try.Resource var2;
            get1: {
                {
                    var2 = self0;
                    break get1;
                }
            }
            final Resource r2 = var2;
            .Try.Resource var4;
            get3: {
                {
                    var4 = mr;
                    break get3;
                }
            }
            final Resource r0 = var4;
            try (r1
r2
r0
final Resource r3 = new Resource("r3");) {
                            System.out.println("block");
                        }
                    }
                    System.out.println("done");
                }

                static class Resource implements AutoCloseable {
                    final String id;

                    Resource(String id) {
                        super();
                        this.id = id;
                        System.out.println(id + " created");
                    }

                    @Override()
                    public void close() {
                        System.out.println(id + " closed");
                    }
                }

                @Pseudo()
                static final class MyResource extends Wrapper<Resource> {

                    MyResource() {
                        super();
                    }

                    Resource get() {
                        return value;
                    }
                }
            }