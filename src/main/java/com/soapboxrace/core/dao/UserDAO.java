package com.soapboxrace.core.dao;

import com.soapboxrace.core.dao.util.BaseDAO;
import com.soapboxrace.core.jpa.UserEntity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class UserDAO extends BaseDAO<UserEntity> {

	@PersistenceContext
	protected void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public UserEntity findById(Long id) {
		UserEntity user = entityManager.find(UserEntity.class, id);

		user.getListOfProfile().size();

		return user;
	}

	public UserEntity findByEmail(String email) {
		TypedQuery<UserEntity> query = entityManager.createNamedQuery("UserEntity.findByEmail", UserEntity.class);
		query.setParameter("email", email);
		
		List<UserEntity> resultList = query.getResultList();
		return !resultList.isEmpty() ? resultList.get(0) : null;
	}

	public UserEntity findByVerifyToken(String token) {
		TypedQuery<UserEntity> query = entityManager.createNamedQuery("UserEntity.findByVerifyToken",
				UserEntity.class);
		query.setParameter("token", token);

		List<UserEntity> resultList = query.getResultList();
		return !resultList.isEmpty() ? resultList.get(0) : null;
	}
}
