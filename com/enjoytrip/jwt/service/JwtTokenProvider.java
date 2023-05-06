	// 유저 정보를 가지고 AccessToken, RefreshToken 을 생성하는 메서드
	public TokenInfo generateToken(Authentication authentication) {
		// 권한 가져오기
		String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));
		log.info("authroities : {}", authorities);
		long now = (new Date()).getTime();
		// Access Token 생성
		Date accessTokenExpiresIn = new Date(now + 86400000);
		String accessToken = Jwts.builder()
				.setSubject(authentication.getName())
				.claim("auth", authorities)
				.setExpiration(accessTokenExpiresIn)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();

		// Refresh Token 생성 230506
		String refreshToken = Jwts
				.builder()
				.setSubject(authentication.getName())
				.setExpiration(new Date(now + 86400000))
				.signWith(key, SignatureAlgorithm.HS256).compact();

		return TokenInfo.builder().grantType("Bearer").accessToken(accessToken).refreshToken(refreshToken).build();
	}

// 230506 : ACCESS 토큰 만 생성하는 것
	public String genereateAccessToken(String id, String role) {
		long now = (new Date()).getTime();
		Date accessTokenExpiresIn = new Date(now + 86400000);
		String accessToken = Jwts.builder()
				.setSubject(id)
				.claim("auth", role)
				.setExpiration(accessTokenExpiresIn)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
		return accessToken;
	}
	
	// 230506 : 리프레시토큰 만 생성하는 것
	public String generateRefreshToken(String id) {
		long now = (new Date()).getTime();
		return Jwts
				.builder()
				.setSubject(id)
				.setExpiration(new Date(now + 86400000))
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	// 230506 JWT 토큰 복호화해서 Date 값만 가져오는 메서드
	public Date getDateFromRefreshToken(String token) {
		log.info("DATE");
		Claims claims = parseClaims(token);
		// Long lastFreshTime = (Long) claims.get("exp"); 
		Integer expValue = (Integer) claims.get("exp");
		Long lastFreshTime = expValue.longValue();
		log.info("lastFreshTime : {}", lastFreshTime);
		return new Date(lastFreshTime*1000);
	}
