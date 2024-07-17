package io.rivrs.geysermodelengine.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.bone.ModelBone;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ModelUtils {

    public static String unstripName(ModelBone bone) {
        String name = bone.getBoneId();
        if (bone.getBlueprintBone().getBehaviors().get("head") != null) {
            if (!bone.getBlueprintBone().getBehaviors().get("head").isEmpty()) return "hi_" + name;
            return "h_" + name;
        }
        return name;
    }

}
