import wn.pseudoclasses.Pseudo;

@Pseudo
final class UnsupportedDecl extends String {

    {
        System.out.println("Hello!");
    }
}