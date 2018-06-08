package com.acuitybotting.db.arango.path_finding.domain;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.Key;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * Created by Zachary Herridge on 5/30/2018.
 */
@Document("SceneEntity")
@Data
public class SceneEntity {

    @Id
    private String id;
    @Key
    private String key;

    private boolean deprecated;

    private String name;
    private String entityID;

    private int plane;
    private int x;
    private int y;

    private int rotation;

    private String[] actions;
}
