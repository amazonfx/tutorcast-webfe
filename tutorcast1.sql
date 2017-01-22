-- MySQL dump 10.13  Distrib 5.5.24, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: tutorcast1
-- ------------------------------------------------------
-- Server version	5.5.24-0ubuntu0.12.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Class`
--

DROP TABLE IF EXISTS `Class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Class` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_name` varchar(100) DEFAULT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `teacher_username` varchar(100) DEFAULT NULL,
  `is_ended` tinyint(1) DEFAULT NULL,
  `course_id` bigint(20) DEFAULT NULL,
  `created_at` bigint(20) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `last_signed_on` bigint(20) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT NULL,
  `is_public` tinyint(1) DEFAULT '0',
  `recorded_public` tinyint(1) DEFAULT '1',
  `teacher_id` bigint(20) DEFAULT NULL,
  `tok_session` varchar(270) DEFAULT NULL,
  `archive_id` varchar(270) DEFAULT NULL,
  `photo_url_large` varchar(512) DEFAULT NULL,
  `photo_url_medium` varchar(512) DEFAULT NULL,
  `photo_url_small` varchar(512) DEFAULT NULL,
  `photo_url_square` varchar(512) DEFAULT NULL,
  `timezone` varchar(10) DEFAULT NULL,
  `max_minutes` int(11) DEFAULT NULL,
  `is_paid` tinyint(1) DEFAULT '0',
  `cost` float DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `video_offset` bigint(20) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `Class_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `Course` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ClassArchive`
--

DROP TABLE IF EXISTS `ClassArchive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ClassArchive` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(20) DEFAULT NULL,
  `archive_id` varchar(190) DEFAULT NULL,
  `video_id` varchar(190) DEFAULT NULL,
  `url` varchar(270) DEFAULT NULL,
  `length` bigint(20) DEFAULT '0',
  `offset` bigint(20) DEFAULT '0',
  `is_teacher` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_videoid` (`video_id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `ClassArchive_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `Class` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ClassUser`
--

DROP TABLE IF EXISTS `ClassUser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ClassUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `is_teacher` tinyint(1) DEFAULT NULL,
  `cost` float DEFAULT NULL,
  `paid` tinyint(1) DEFAULT '0',
  `user_email` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `class_id` (`class_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `ClassUser_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `User` (`id`),
  CONSTRAINT `ClassUser_ibfk_3` FOREIGN KEY (`class_id`) REFERENCES `Class` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Course`
--

DROP TABLE IF EXISTS `Course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_name` varchar(100) DEFAULT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `teacher_username` varchar(100) DEFAULT NULL,
  `is_ended` tinyint(1) DEFAULT NULL,
  `created_at` bigint(20) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `last_signed_on` bigint(20) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CourseUser`
--

DROP TABLE IF EXISTS `CourseUser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CourseUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) DEFAULT NULL,
  `student_id` bigint(20) DEFAULT NULL,
  `is_teacher` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `course_id` (`course_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `CourseUser_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `Course` (`id`),
  CONSTRAINT `CourseUser_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `User` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Credential`
--

DROP TABLE IF EXISTS `Credential`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Credential` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fname` varchar(100) DEFAULT NULL,
  `lname` varchar(100) DEFAULT NULL,
  `username` varchar(100) DEFAULT NULL,
  `linkedin_verified` tinyint(1) DEFAULT NULL,
  `credential_type` int(11) DEFAULT NULL,
  `school` varchar(256) DEFAULT NULL,
  `degree` varchar(40) DEFAULT NULL,
  `year_attained` int(11) DEFAULT NULL,
  `degree_rank` int(11) DEFAULT NULL,
  `skill_name` varchar(256) DEFAULT NULL,
  `proficiency_level` varchar(60) DEFAULT NULL,
  `created_at` bigint(20) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `last_signed_on` bigint(20) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `credential_order` int(11) DEFAULT NULL,
  `concentration` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `Credential_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `User` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event`
--

DROP TABLE IF EXISTS `Event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `source_user_id` bigint(20) DEFAULT NULL,
  `source_user_name` varchar(100) DEFAULT NULL,
  `source_user_first_name` varchar(100) DEFAULT NULL,
  `source_user_last_name` varchar(100) DEFAULT NULL,
  `event_type` int(11) DEFAULT NULL,
  `course_name` varchar(500) DEFAULT NULL,
  `course_id` bigint(20) DEFAULT NULL,
  `target_user_id` bigint(20) DEFAULT NULL,
  `target_user_name` varchar(100) DEFAULT NULL,
  `target_user_first_name` varchar(100) DEFAULT NULL,
  `target_user_last_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Material`
--

DROP TABLE IF EXISTS `Material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Material` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `material_name` varchar(256) DEFAULT NULL,
  `link` varchar(512) DEFAULT NULL,
  `created_at` bigint(20) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `last_signed_on` bigint(20) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT NULL,
  `class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `class_class_id` (`class_id`),
  CONSTRAINT `Material_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `Class` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PendingClassUser`
--

DROP TABLE IF EXISTS `PendingClassUser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PendingClassUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_id` bigint(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `cost` float DEFAULT NULL,
  `paid` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `PendingClassUser_ibfk_3` FOREIGN KEY (`class_id`) REFERENCES `Class` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Tag`
--

DROP TABLE IF EXISTS `Tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag_2` (`tag`),
  KEY `tag` (`tag`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TagClass`
--

DROP TABLE IF EXISTS `TagClass`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TagClass` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_id` bigint(20) DEFAULT NULL,
  `class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tag_id` (`tag_id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `TagClass_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `Class` (`id`),
  CONSTRAINT `TagClass_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `Tag` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `User` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fname` varchar(100) DEFAULT NULL,
  `lname` varchar(100) DEFAULT NULL,
  `username` varchar(100) DEFAULT NULL,
  `password` varchar(100) DEFAULT NULL,
  `user_email` varchar(100) DEFAULT NULL,
  `canonnical_email` varchar(100) DEFAULT NULL,
  `is_facebook` tinyint(1) DEFAULT '0',
  `created_at` bigint(20) DEFAULT NULL,
  `updated_at` bigint(20) DEFAULT NULL,
  `last_signed_on` bigint(20) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT NULL,
  `linkedin_verified` tinyint(1) DEFAULT NULL,
  `is_teacher` tinyint(1) DEFAULT NULL,
  `location` varchar(256) DEFAULT 'Location Not Specified',
  `facebook_id` bigint(20) DEFAULT NULL,
  `photo_url_large` varchar(512) DEFAULT NULL,
  `photo_url_medium` varchar(512) DEFAULT NULL,
  `photo_url_small` varchar(512) DEFAULT NULL,
  `photo_url_square` varchar(512) DEFAULT NULL,
  `recovery_id` varchar(256) DEFAULT NULL,
  `phone` varchar(40) DEFAULT 'Phone Not Specified',
  `title` varchar(256) DEFAULT 'Tutorcast Member',
  `about_me` varchar(1024) DEFAULT 'I haven''t had a chance to fill this out yet.',
  PRIMARY KEY (`id`),
  KEY `username_index` (`username`),
  KEY `facebook_id_index` (`facebook_id`),
  KEY `recover_id_index` (`recovery_id`(255))
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UserCredential`
--

DROP TABLE IF EXISTS `UserCredential`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UserCredential` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `credential_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `credential_id` (`credential_id`),
  CONSTRAINT `UserCredential_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `User` (`id`),
  CONSTRAINT `UserCredential_ibfk_2` FOREIGN KEY (`credential_id`) REFERENCES `Credential` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UserUser`
--

DROP TABLE IF EXISTS `UserUser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UserUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `friender_id` bigint(20) DEFAULT NULL,
  `friendee_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `friender_id` (`friender_id`),
  KEY `friendee_id` (`friendee_id`),
  CONSTRAINT `UserUser_ibfk_1` FOREIGN KEY (`friender_id`) REFERENCES `User` (`id`),
  CONSTRAINT `UserUser_ibfk_2` FOREIGN KEY (`friendee_id`) REFERENCES `User` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-12-16  7:31:49
