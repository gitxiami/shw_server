package top.yunshu.shw.server.controller.teacher;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import top.yunshu.shw.server.entity.*;
import top.yunshu.shw.server.exception.FileException;
import top.yunshu.shw.server.model.WorkDetailsModel;
import top.yunshu.shw.server.model.WorkModel;
import top.yunshu.shw.server.service.config.ConfigService;
import top.yunshu.shw.server.service.file.FileService;
import top.yunshu.shw.server.service.group.GroupService;
import top.yunshu.shw.server.service.upload.UploadService;
import top.yunshu.shw.server.service.work.WorkService;
import top.yunshu.shw.server.util.FileUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 教师控制器
 *
 * @author itning
 * @author shulu
 * @date 2018/12/21
 */
@Api(tags = {"教师接口"})
@RestController
@RequestMapping("/teacher")
public class TeacherController {
    private static final Logger logger = LoggerFactory.getLogger(TeacherController.class);
    public static volatile Map<String, String> packMap = new HashMap<>(16);

    private final GroupService groupService;

    private final WorkService workService;

    private final ModelMapper modelMapper;

    private final FileService fileService;

    private final UploadService uploadService;

    private final ConfigService configService;

    @Autowired
    public TeacherController(GroupService groupService, WorkService workService, ModelMapper modelMapper, FileService fileService, UploadService uploadService, ConfigService configService) {
        this.groupService = groupService;
        this.workService = workService;
        this.modelMapper = modelMapper;
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.configService = configService;
    }

    /**
     * 教师获取创建的所有群组
     *
     * @return ResponseEntity
     */
    @ApiOperation(value = "教师获取创建的所有群组", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            response = Group.class, responseContainer = "List")
    @GetMapping("/groups")
    public Callable<ResponseEntity<RestModel>> getTeacherCreateGroups(@ApiIgnore LoginUser loginUser,
                                                                      @ApiParam("分页信息") @PageableDefault(size = 20, sort = {"gmtCreate"}, direction = Sort.Direction.DESC)
                                                                              Pageable pageable) {
        return () -> ResponseEntity.ok(new RestModel<>(groupService.findTeacherAllGroups(loginUser.getNo(), pageable)));
    }

    /**
     * 新建群组
     *
     * @param groupName 教师添加的组名
     * @return ResponseEntity
     */
    @ApiOperation(value = "新建群组", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, response = Group.class)
    @PostMapping("/group")
    public ResponseEntity<Group> addGroup(@ApiIgnore LoginUser loginUser,
                                          @ApiParam(value = "教师添加的组名", required = true) @RequestParam String groupName) {
        logger.debug("add group , groupName: " + groupName);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(groupName, loginUser.getName(), loginUser.getNo()));
    }

    /**
     * 更新群组
     *
     * @param name 修改的新群组名
     * @param id   群组id
     * @return ResponseEntity
     */
    @ApiOperation("更新群组")
    @PatchMapping("/group/{id}/{name}")
    public ResponseEntity<Void> updateGroupName(@ApiIgnore LoginUser loginUser,
                                                @ApiParam(value = "群组id", required = true) @PathVariable String id,
                                                @ApiParam(value = "修改的新群组名", required = true) @PathVariable String name) {
        logger.debug("update group , name: " + name);
        groupService.updateGroup(id, name, loginUser.getNo());
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除群组
     *
     * @param id 要删除的群组ID
     */
    @ApiOperation("删除群组")
    @DeleteMapping("/group/{id}")
    public ResponseEntity<Void> deleteGroup(@ApiIgnore LoginUser loginUser,
                                            @ApiParam(value = "要删除的群组ID", required = true) @PathVariable String id) {
        logger.debug("delete group , id: " + id);
        groupService.deleteGroup(id, loginUser.getNo());
        return ResponseEntity.noContent().build();
    }

    /**
     * 查询教师是否有群组
     *
     * @return ResponseEntity
     */
    @ApiOperation(value = "查询教师是否有群组", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, response = RestModel.class)
    @GetMapping("/group/exist")
    public Callable<ResponseEntity<RestModel>> isTeacherHaveAnyGroup(@ApiIgnore LoginUser loginUser) {
        logger.debug("is Teacher Have Any Group");
        return () -> ResponseEntity.ok(new RestModel<>(groupService.isHaveAnyGroup(loginUser.getNo())));
    }

    /**
     * 获取教师所有作业
     *
     * @return ResponseEntity
     */
    @ApiOperation(value = "获取教师所有作业", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            response = WorkModel.class, responseContainer = "List")
    @GetMapping("/works")
    public Callable<ResponseEntity<RestModel>> getTeacherWorks(@ApiIgnore LoginUser loginUser,
                                                               @ApiParam("分页信息") @PageableDefault(size = 20, sort = {"gmtCreate"}, direction = Sort.Direction.DESC)
                                                                       Pageable pageable) {
        logger.debug("get teacher works");
        return () -> ResponseEntity.ok(new RestModel<>(workService.getTeacherAllWork(loginUser.getNo(), pageable)));
    }

    /**
     * 根据群ID获取作业
     *
     * @param groupId 群ID
     * @return ResponseEntity
     */
    @ApiOperation(value = "根据群ID获取作业", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            response = WorkModel.class, responseContainer = "List")
    @GetMapping("/work/{groupId}")
    public Callable<ResponseEntity<RestModel>> getTeacherWork(@ApiIgnore LoginUser loginUser,
                                                              @ApiParam(value = "群ID", required = true) @PathVariable String groupId) {
        logger.debug("get teacher work");
        return () -> ResponseEntity.ok(new RestModel<>(modelMapper.map(workService.getTeacherWork(loginUser.getNo(), groupId), new TypeToken<List<WorkModel>>() {
        }.getType())));
    }

    /**
     * 添加作业
     *
     * @param workName 作业名
     * @param groupId  群ID
     * @return ResponseEntity
     */
    @ApiOperation(value = "添加作业", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            response = Work.class)
    @PostMapping("/work")
    public ResponseEntity<Work> addWork(@ApiIgnore LoginUser loginUser,
                                        @ApiParam(value = "作业名", required = true) @RequestParam String workName,
                                        @ApiParam(value = "群ID", required = true) @RequestParam String groupId) {
        logger.debug("add work , workName: " + workName);
        return ResponseEntity.status(HttpStatus.CREATED).body(workService.createWork(workName, groupId, "", true));
    }

    /**
     * 更新作业启用状态
     *
     * @param workId  作业ID
     * @param enabled 开启状态
     * @return ResponseEntity
     */
    @ApiOperation("更新作业启用状态")
    @PatchMapping("/work/{workId}/{enabled}")
    public ResponseEntity<Void> updateWorkEnabled(@ApiIgnore LoginUser loginUser,
                                                  @ApiParam(value = "作业ID", required = true) @PathVariable String workId,
                                                  @ApiParam(value = "开启状态", required = true) @PathVariable String enabled) {
        logger.debug("up work , work id: " + workId + " enabled: " + enabled);
        workService.changeEnabledWord(workId, Boolean.parseBoolean(enabled));
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除作业
     *
     * @param workId 作业ID
     * @return ResponseEntity
     */
    @ApiOperation("删除作业")
    @DeleteMapping("/work/{workId}")
    public ResponseEntity<Void> deleteWork(@ApiIgnore LoginUser loginUser,
                                           @ApiParam(value = "作业ID", required = true) @PathVariable String workId) {
        logger.debug("del work , work id: " + workId);
        workService.delWork(workId, loginUser.getNo());
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取作业详情
     *
     * @param workId 作业ID
     * @return ResponseEntity
     */
    @ApiOperation(value = "获取作业详情", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            response = WorkDetailsModel.class, responseContainer = "List")
    @GetMapping("/work_detail/{workId}")
    public Callable<ResponseEntity<RestModel>> getTeacherWorkDetails(@ApiIgnore LoginUser loginUser,
                                                                     @ApiParam(value = "作业ID", required = true) @PathVariable String workId,
                                                                     @ApiParam("分页信息") @PageableDefault(size = 20, sort = {"studentNumber"}, direction = Sort.Direction.ASC)
                                                                             Pageable pageable) {
        logger.debug("get teacher work detail, work id " + workId);
        return () -> ResponseEntity.ok(new RestModel<>(workService.getWorkDetailByWorkId(loginUser.getNo(), workId, pageable)));
    }

    /**
     * 打包
     *
     * @param workId 作业ID
     * @return ResponseEntity
     */
    @ApiOperation(value = "打包", notes = "下载所有作业之前必须先调用该API")
    @GetMapping("/pack/{workId}")
    public ResponseEntity<RestModel> pack(@ApiIgnore LoginUser loginUser,
                                          @ApiParam(value = "作业ID", required = true) @PathVariable String workId) {
        logger.debug("get teacher work detail, work id " + workId);
        String s = packMap.get(workId);
        if (s == null) {
            fileService.unpackFiles(workId);
            return ResponseEntity.ok(new RestModel<>("START"));
        } else {
            if ("OK".equals(s)) {
                packMap.remove(workId);
                return ResponseEntity.ok(new RestModel<>("OK"));
            } else if (s.contains("ERROR")) {
                packMap.remove(workId);
                return ResponseEntity.ok(new RestModel<>(s));
            } else {
                return ResponseEntity.ok(new RestModel<>(s));
            }
        }
    }

    /**
     * 下载所有
     *
     * @param range    请求头
     * @param workId   作业ID
     * @param response {@link HttpServletResponse}
     */
    @ApiOperation("下载所有")
    @GetMapping("/down/{workId}")
    public void downloadAllFile(@ApiParam(value = "Range请求头(用于判断是否需要支持多线程下载和断点续传)", required = true) @RequestHeader(required = false) String range,
                                @ApiParam(value = "作业ID", required = true) @PathVariable String workId,
                                @ApiIgnore HttpServletResponse response) {
        logger.debug("down file, work id: " + workId);
        long sum = uploadService.getUploadSum(workId);
        String tempFilePath = configService.getConfig(Config.ConfigKey.TEMP_DIR).orElse(System.getProperty("java.io.tmpdir")) + File.separator + workId + sum + ".zip";
        File file = new File(tempFilePath);
        if (file.canRead()) {
            FileUtils.breakpointResume(file, "application/octet-stream", range, response);
        } else {
            throw new FileException("文件不可读", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
