package com.github.murataykanat.toybox.dbo.mappers.user;

import com.github.murataykanat.toybox.dbo.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet resultSet, int i) throws SQLException {
        User user = new User();
        user.setEmail(resultSet.getString("email"));
        user.setEnabled(resultSet.getBoolean("enabled"));
        user.setAccountNonExpired(resultSet.getBoolean("account_non_expired"));
        user.setAccountNonLocked(resultSet.getBoolean("account_non_locked"));
        user.setCredentialsNonExpired(resultSet.getBoolean("credentials_non_expired"));
        user.setLastname(resultSet.getString("lastname"));
        user.setName(resultSet.getString("name"));
        user.setUsername(resultSet.getString("username"));
        user.setAvatarPath(resultSet.getString("avatar_path"));
        return user;
    }
}
