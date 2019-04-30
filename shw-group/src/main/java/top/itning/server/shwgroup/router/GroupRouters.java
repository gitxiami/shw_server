package top.itning.server.shwgroup.router;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import top.itning.server.shwgroup.entity.Group;
import top.itning.server.shwgroup.handler.GroupHandler;
import top.itning.server.shwgroup.service.GroupService;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author itning
 * @date 2019/4/29 14:26
 */
@Configuration
public class GroupRouters {
    private final GroupService groupService;

    @Autowired
    public GroupRouters(GroupService groupService) {
        this.groupService = groupService;
    }

    @Bean
    RouterFunction<ServerResponse> userRouter(GroupHandler groupHandler) {
        return nest(
                all(),
                route()
                        .POST("/", groupHandler::addGroup)
                        .GET("/groups", groupHandler::getTeacherCreateGroups)
                        .GET("/exist", groupHandler::isTeacherHaveAnyGroup)
                        .DELETE("/{id}", groupHandler::deleteGroup)
                        .PATCH("/{id}/{name}", groupHandler::updateGroupName)
                        .build()
        )
                .andNest(
                        path("/internal"),
                        route()
                                .GET("/findOneGroupById/{id}", serverRequest -> ServerResponse.ok().body(groupService.findOneGroupById(serverRequest.pathVariable("id")), Group.class))
                                .build()
                );
    }
}
