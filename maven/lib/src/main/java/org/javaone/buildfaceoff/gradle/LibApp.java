package org.javaone.buildfaceoff.gradle;

import lombok.Data;
import org.javaone.buildfaceoff.AppC;

/**
 * @author jbaruch
 * @since 9/16/16
 */
@Data
public class LibApp {

    public String getLibC(){
        return new AppC().getStrV2();
    }
}
