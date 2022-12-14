
public class Switch {

    public Switch() {
        super();
    }

    public static void main(String[] args) {
        int x = (args.length + 10);
        {
            int self0 = args.length;
            int var2;
            get1: {
                {
                    var2 = self0;
                    break get1;
                }
            }
            switch (var2) {
            case 1:
                incByte3: {
                    {
                        ;
                        ;
                        int var24;
                        getByte1: {
                            System.out.printf("getByte(%d)%n", 1);
                            {
                                var24 = x >> 8 & 255;
                                break getByte1;
                            }
                        }
                        int var45 = var24 + 1;
                        setByte5: {
                            ;
                            x &= -65281;
                            x |= (var45 & 255) << 8;
                        }
                    }
                }
                {
                    int var7;
                    get6: {
                        {
                            var7 = x;
                            break get6;
                        }
                    }
                    System.out.println(var7);
                }
                break;

            case (int)2:

            case (int)3 >> 0 & 255:
                incByte14: {
                    {
                        ;
                        ;
                        int var215;
                        getByte1: {
                            System.out.printf("getByte(%d)%n", 2);
                            {
                                var215 = x >> 16 & 255;
                                break getByte1;
                            }
                        }
                        int var416 = var215 + 1;
                        setByte5: {
                            ;
                            x &= -16711681;
                            x |= (var416 & 255) << 16;
                        }
                    }
                }
                {
                    int var18;
                    get17: {
                        {
                            var18 = x;
                            break get17;
                        }
                    }
                    System.out.println(var18 + 1);
                }
                break;

            default:
                {
                    int var20;
                    get19: {
                        {
                            var20 = x;
                            break get19;
                        }
                    }
                    System.out.println(var20);
                }

            }
        }
    }
}