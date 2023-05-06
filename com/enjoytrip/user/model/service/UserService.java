package com.enjoytrip.user.model.service;



import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enjoytrip.jwt.service.JwtTokenProvider;
import com.enjoytrip.jwt.service.TokenInfo;
import com.enjoytrip.user.entity.RefreshToken;
import com.enjoytrip.user.entity.UserDto;
import com.enjoytrip.user.repository.RefreshTokenRepository;
import com.enjoytrip.user.repository.UserRespository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
// 로그인 메서드 구현
public class UserService {
	
	private final UserRespository userRespository;
	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;
	
	@Autowired
	private final RefreshTokenRepository refreshTokenRepository;
	
	// 단순 로그인
	@Transactional
	public TokenInfo login(String id, String password) {
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(id, password);
 
        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
 
        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
        
        // 4. JWT 토큰 생성 시, refresh_token 에 정보 저장 230506
        RefreshToken token = new RefreshToken();
        token.setId(id);
        token.setRefreshtoken(tokenInfo.getRefreshToken());
        refreshTokenRepository.save(token);
        
        return tokenInfo;
	}
	
	// 리프레시 토큰이 기간 지났는지 확인
	// 리프레시 토큰 기간이 지나지 않았으면 리프레시 토큰 값을 Return
	public RefreshToken validateRefreshToken(String token) {
		log.info("토큰 기간 확인");
	    RefreshToken refreshToken = findRefrestokenByrefreshtoken(token);
	    log.info("refreshtoken : {}", refreshToken);
	    Date tokenDate = jwtTokenProvider.getDateFromRefreshToken(token);
	    log.info("리프레시 토큰 기간 : {}", tokenDate);
	    if (tokenDate.toInstant().isBefore(Instant.now())) {
	        refreshTokenRepository.delete(refreshToken);
	        throw new RuntimeException("Refresh token has expired");
	    }
	    
	    return refreshToken;
	}
	
	// 리프레시 토큰 Revoke를 위한 메서드
	public void revokeRefreshToken(String token) {
		refreshTokenRepository.deleteByrefreshtoken(token);
	}
	
	// 리프레시토큰으로 리프레시 토큰 값 찾기. -> 리프레시 토큰 존재 여부 찾기 위함
	public RefreshToken findRefrestokenByrefreshtoken(String refreshtoken) {
		log.info("findRefrestokenByrefreshtoken : {}", refreshtoken);
		return refreshTokenRepository.findByrefreshtoken(refreshtoken)
				.orElseThrow( () -> new RuntimeException("리프레시 토큰이 없습니다."));
	}
	
	//	ID로 ROLE 가져오기 230506
	@Transactional
    public String findRolesById(String userId) {
        List<String> roles = userRespository.findRolesById(userId);
        return roles.get(0);
    }
	
	@Transactional
	public String findId(String email) throws Exception {
		if(userRespository.findByemail(email).isPresent()) {
			String id = userRespository.findByemail(email).get().getId();
			log.info("찾은 아이디 : {}", id);
			return id;
		} else {
			throw new Exception("존재하지 않은 이메일 입니다.");
		}
	}
	
	@Transactional
	public String findpw(String id, String email) throws Exception {
		if(userRespository.findByemail(id).isPresent()) {
			if(userRespository.findByid(id).equals(userRespository.findByemail(email))) {
				String pw = userRespository.findByemail(id).get().getPassword();
				return pw;
			} else {
				throw new Exception("아이디와 이메일이 매칭되지 않습니다.");
			}
		} else {
			throw new Exception("아이디가 존재하지 않습니다.");
		}
	}
	
	// 단순 회원가입
	@Transactional
	public void join(UserDto userDto) throws Exception {
		
		if(userRespository.findByid(userDto.getId()).isPresent()) {
			throw new Exception("이미 존재하는 아이디 입니다.");
		}
		
		try {
//			log.info(userDto.toString());
//			log.info(userDto.toEntity().getRoles().get(0));
//			userRespository.save(userDto.toEntity());
			userRespository.save(userDto.toEntity());			
			
		} catch (Exception e) {
			e.printStackTrace();
			log.info("user service join error ! !");
		}
	}
	
//	1. **/user/join (DELETE) (회원 탈퇴)**
	@Transactional
	public int deleteUser(String id) throws Exception {
		Optional<UserDto> user = userRespository.findByid(id);
		log.info(user.get().toString());
		if(user.isPresent()) {
			userRespository.delete(user.get());
			return 1;
		}
		return 0;
	}
	
//	2. **/user/modify (PUT) (정보 수정)**
	@Transactional
	public int updateUser(String id, UserDto requestUser) throws Exception{
		Optional<UserDto> user = userRespository.findByid(id);
		if(!user.isPresent()) return 0;
		
		UserDto updateUser = user.get();
		log.info("UPDATE to.. : {}", updateUser.getEmail());
		updateUser.setNickname(requestUser.getNickname());
		updateUser.setEmail(requestUser.getEmail());
		updateUser.setPassword(requestUser.getPassword());
		updateUser.setSido(requestUser.getSido());
		updateUser.setGugun(requestUser.getGugun());
		log.info("UPDATE 전 : {}", updateUser.getEmail());
		userRespository.save(updateUser);
		return 1;
	}
	
//	3. **/user/review/board (GET)**
	// 본인이 작성한 리뷰 확인
	@Transactional
	List<String> listReview(Map<String, Object> map) throws Exception{
		//<ReviewDto>
		return null;
	}
	
//	4. **/user/review/hotplace (GET)**
	// 본인이 등록한 핫플레이스 확인
	@Transactional
	List<String> listHotplace(Map<String, Object> map) throws Exception {
		//<HotplaceDto>
		return null;
		
	}
}
