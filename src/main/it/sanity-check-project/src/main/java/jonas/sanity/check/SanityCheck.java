package jonas.sanity.check;

import java.util.ArrayList;
import akka.http.*;


public class SanityCheck {

    

    public static void main(String[] args){
        System.out.println("Hello World");

        ArrayList<Integer> dependencyTest = new ArrayList<>();

        for(int i = 0; i < 100; i++){
            dependencyTest.add(i);
        }

        System.out.println(dependencyTest.toString());
    }


}
