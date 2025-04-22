package test;

import com.minit.ContainerEvent;
import com.minit.ContainerListener;

public class TestListener implements ContainerListener{

    @Override
    public void containerEvent(ContainerEvent event) {
        System.out.println(event);
    }

}
