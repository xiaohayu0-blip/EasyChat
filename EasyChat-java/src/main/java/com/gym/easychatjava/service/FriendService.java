package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.FriendRequestDTO;
import com.gym.easychatjava.dto.UpdateRemarkDTO;
import com.gym.easychatjava.vo.FriendRequestVO;
import com.gym.easychatjava.vo.FriendVO;
import com.gym.easychatjava.dto.HandleRequestDTO;

import java.util.List;

public interface FriendService {

    /** 发送好友申请 */
    void sendRequest(FriendRequestDTO dto);

    /**收到好友申请*/
    List<FriendRequestVO> listReceivedRequests();

    /**处理好友申请*/
    void handleRequest(Long requestId, HandleRequestDTO dto);

    /**好友列表*/
    List<FriendVO> listFriends();

    /**删除好友*/
    void deleteFriend(Long friendId);

    /** 设置/清除好友备注名 */
    void updateRemark(UpdateRemarkDTO dto);

    void blockFriend(Long friendId);

    void unblockFriend(Long friendId);

    Integer getUnreadRequestCount();

}
