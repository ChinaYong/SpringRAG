package com.aiplus.spring_rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.aiplus.spring_rag.entity.User;
import com.aiplus.spring_rag.service.UserService;

@SpringBootTest
class SpringRagApplicationTests {

	@Autowired
	UserService userService;

	@Autowired
	private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

	@Test
	void testRedisConnection() {
		stringRedisTemplate.opsForValue().set("test_boss", "SpringRAG_Reids_Works");

		String value = stringRedisTemplate.opsForValue().get("test_boss");

		System.out.println("从Redis中获取的值：" + value);
	}
	
	@Test
	void contextLoads() {
	}

	@Test
	void testInsertUser() {
		User user = new User();
		user.setUsername("testUser");
		user.setPassword("pass");

		boolean result = userService.save(user);

		System.out.println("插入结果:" + result);

		System.out.println("生成的用户ID: " + user.getId());
	}

}
