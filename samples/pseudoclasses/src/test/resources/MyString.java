import wn.pseudoclasses.Pseudo;

@Pseudo
final class MyString extends String {

    public String asString() {
        return this.toString();
    }
}
