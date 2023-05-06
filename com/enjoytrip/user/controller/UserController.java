	// 230506 : 로그인 고침
	@PostMapping("/newlogin") 
	public ResponseEntity<TokenInfo> authorize(@RequestBody UserLoginRequestDto userLoginRequestDto){
		log.info("new LOGIN !! ");
		String id = userLoginRequestDto.getId();
		String password = userLoginRequestDto.getPassword();
		TokenInfo tokenInfo = userService.login(id, password);
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Authorization", "Bearer " + tokenInfo.getAccessToken());
		
		log.info("tokeninfo : {}", tokenInfo);
//		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		log.info("authentication : {}", authentication.toString());
		
		return new ResponseEntity<TokenInfo>(new TokenInfo(tokenInfo.getGrantType(), tokenInfo.getAccessToken(), tokenInfo.getRefreshToken()), httpHeaders, HttpStatus.OK);
	}
	
	// 230506
	@PostMapping("/refresh-token")
	public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {
		String requestRefreshToken = requestDto.getRefreshToken();
		log.info("{}", requestRefreshToken);
		try {
			userService.validateRefreshToken(requestRefreshToken);
			String id = userService.findRefrestokenByrefreshtoken(requestRefreshToken).getId();
			log.info("id : {}", id);
			// String rtoken = jwtTokenProvider.generateRefreshToken(id);
			String role = "ROLE_" + userService.findRolesById(id);
			String atoken = jwtTokenProvider.genereateAccessToken(id, role);
			
			return ResponseEntity.ok(new TokenInfo("Bearer", atoken, requestRefreshToken));
		} catch (Exception e) {
			new TokenRefreshException(requestRefreshToken, " -> 이 리프레시 토큰은 DB에 존재하지 않습니다!...");
		}
		log.info("리프레시 토큰 에러 !");
		return null;
