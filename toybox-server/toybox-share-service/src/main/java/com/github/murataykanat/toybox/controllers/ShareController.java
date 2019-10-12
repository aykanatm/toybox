package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.schema.share.InternalShareRequest;
import com.github.murataykanat.toybox.utilities.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@RefreshScope
@RestController
public class ShareController {
    private static final Log _logger = LogFactory.getLog(ShareController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private SelectionUtils selectionUtils;
    @Autowired
    private ShareUtils shareUtils;

    @Autowired
    private ExternalSharesRepository externalSharesRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/download/{externalShareId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadExternalShare(@PathVariable String externalShareId, HttpServletResponse response) throws IOException {
        try{
            if(StringUtils.isNotBlank(externalShareId)){
                List<ExternalShare> externalSharesById = externalSharesRepository.getExternalSharesById(externalShareId);
                if(!externalSharesById.isEmpty()){
                    if(externalSharesById.size() == 1){
                        ExternalShare externalShare = externalSharesById.get(0);

                        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

                        if(externalShare.getEnableExpireExternal().equalsIgnoreCase("Y")){
                            Date expirationDate = externalShare.getExpirationDate();
                            if(expirationDate != null){
                                Calendar cal = Calendar.getInstance();
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                Date today = cal.getTime();

                                _logger.debug("Expiration date: " + formatter.format(expirationDate));
                                _logger.debug("Today: " + formatter.format(today));

                                if(!today.before(expirationDate)){
                                    String errorMessage = "The shared content is expired. If you still would like to download this content, please contact the person who shared this content with you.";
                                    _logger.error(errorMessage);

                                    Context context = new Context();
                                    context.setVariable("error_code", HttpStatus.UNAUTHORIZED.value());
                                    context.setVariable("error_code_reason", HttpStatus.UNAUTHORIZED.getReasonPhrase());
                                    context.setVariable("error_message", errorMessage);
                                    templateEngine.process("externalshare_error", context, response.getWriter());

                                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                                }
                            }
                            else{
                                throw new IllegalArgumentException("Expiration date is null!");
                            }
                        }

                        if(externalShare.getEnableUsageLimit().equalsIgnoreCase("Y")){
                            int maxNumberOfHits = externalShare.getMaxNumberOfHits();
                            if(maxNumberOfHits <= 0){
                                String errorMessage = "The maximum number of uses exceeded the set amount!";
                                _logger.error(errorMessage);

                                Context context = new Context();
                                context.setVariable("error_code", HttpStatus.UNAUTHORIZED.value());
                                context.setVariable("error_code_reason", HttpStatus.UNAUTHORIZED.getReasonPhrase());
                                context.setVariable("error_message", "The shared content is expired. If you still would like to download this content, please contact the person who shared this content with you.");
                                templateEngine.process("externalshare_error", context, response.getWriter());

                                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                            }
                        }

                        String downloadFilePath =  exportStagingPath + File.separator + externalShare.getJobId() + File.separator + "Download.zip";

                        File archiveFile = new File(downloadFilePath);
                        if(archiveFile.exists()){
                            if(externalShare.getNotifyWhenDownloaded().equalsIgnoreCase("Y")){
                                // Send notification manually (because we don't have a session)
                                String toUsername = externalShare.getUsername();
                                String fromUsername = "system";

                                Notification notification = new Notification();
                                notification.setUsername(toUsername);
                                notification.setNotification("The assets you externally shared were downloaded.");
                                notification.setIsRead("N");
                                notification.setDate(new Date());
                                notification.setFrom(fromUsername);

                                rabbitTemplate.convertAndSend(ToyboxConstants.TOYBOX_NOTIFICATION_EXCHANGE,"toybox.notification." + System.currentTimeMillis(), notification);
                            }

                            if(externalShare.getEnableUsageLimit().equalsIgnoreCase("Y")){
                                int maxNumberOfHits = externalShare.getMaxNumberOfHits();
                                maxNumberOfHits--;
                                externalSharesRepository.updateMaxUsage(maxNumberOfHits, externalShareId);
                            }

                            InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Content-disposition", "attachment; filename=Download.zip");
                            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                        }
                        else{
                            throw new IOException("File path '" + downloadFilePath + "' is not valid!");
                        }
                    }
                    else{
                        throw new IllegalArgumentException("There are multiple external shares with ID '" + externalShareId + "'!");
                    }
                }
                else{
                    String errorMessage = "The requested content cannot be found. If you still would like to download this content, please contact the person who shared this content with you.";
                    _logger.error(errorMessage);

                    Context context = new Context();
                    context.setVariable("error_code", HttpStatus.NOT_FOUND.value());
                    context.setVariable("error_code_reason", HttpStatus.NOT_FOUND.getReasonPhrase());
                    context.setVariable("error_message", errorMessage);
                    templateEngine.process("externalshare_error", context, response.getWriter());

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "The ID that is provided to download this content is blank. Please provide a valid ID.";
                _logger.error(errorMessage);

                Context context = new Context();
                context.setVariable("error_code", HttpStatus.BAD_REQUEST.value());
                context.setVariable("error_code_reason", HttpStatus.BAD_REQUEST.getReasonPhrase());
                context.setVariable("error_message", errorMessage);
                templateEngine.process("externalshare_error", context, response.getWriter());

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An unexpected error occurred while downloading the shared content. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            Context context = new Context();
            context.setVariable("error_code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            context.setVariable("error_code_reason", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            context.setVariable("error_message", "An unexpected error occurred while downloading the shared content, please try again later. If the problem persists, please contact the person who shared this content with you.");
            templateEngine.process("externalshare_error", context, response.getWriter());

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(externalShareRequest != null){
                        SelectionContext selectionContext = externalShareRequest.getSelectionContext();
                        if(selectionUtils.isSelectionContextValid(selectionContext)){
                            externalShareResponse = shareUtils.createExternalShare(user, externalShareRequest, selectionContext, session);
                            return new ResponseEntity<>(externalShareResponse, HttpStatus.CREATED);
                        }
                        else{
                            String errorMessage = "Selection context is not valid!";
                            _logger.error(errorMessage);

                            externalShareResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
                        }
                    }
                    else{
                        String errorMessage = "External share request is null!";
                        _logger.error(errorMessage);

                        externalShareResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    throw new IllegalArgumentException("User is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                externalShareResponse.setMessage(errorMessage);

                return new ResponseEntity<>(externalShareResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while sharing assets externally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            externalShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(externalShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/internal", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> createInternalShare(Authentication authentication, HttpSession session, @RequestBody InternalShareRequest internalShareRequest) {
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(internalShareRequest != null){
                        SelectionContext selectionContext = internalShareRequest.getSelectionContext();
                        if(selectionUtils.isSelectionContextValid(selectionContext)){
                            shareUtils.createInternalShare(user, internalShareRequest, selectionContext, session);

                            genericResponse.setMessage("Internal share created successfully!");
                            return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                        }
                        else{
                            String errorMessage = "Selection context is not valid!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                        }
                    }
                    else {
                        String errorMessage = "Internal share request is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    throw new IllegalArgumentException("User is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while sharing assets internally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}