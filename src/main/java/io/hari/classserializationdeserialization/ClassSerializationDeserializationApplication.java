package io.hari.classserializationdeserialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SpringBootApplication
@RequiredArgsConstructor
public class ClassSerializationDeserializationApplication {
	private final DemoEntityRepo demoEntityRepo;

	public static void main(String[] args) {
		SpringApplication.run(ClassSerializationDeserializationApplication.class, args);
	}

	@PostConstruct
	public void foo(){
		System.out.println("ClassSerializationDeserializationApplication.foo");
		BasicDepartment department = new BasicDepartment();
		department.setLocation("dep bangalore");
		department.setDepartmentName("CAR");
		department.setLocation("location 123");

		DetailedDepartment detailedDepartment = new DetailedDepartment();
		detailedDepartment.setDepartmentName("dep name skoda");
		detailedDepartment.setNumberOfEmployees(50);

		BasicContactInfo basicContactInfo = new BasicContactInfo();
		basicContactInfo.setPhoneNumber("1244");
		basicContactInfo.setEmail("@gmail");
		basicContactInfo.setAddress("address 123");
		basicContactInfo.setDepartment(department);


		DetailedContactInfo detailedContactInfo = new DetailedContactInfo();
		detailedContactInfo.setAddress("addr 234");
		detailedContactInfo.setEmail("@gmail");
		detailedContactInfo.setPhoneNumber("12312312");
		detailedContactInfo.setAdditionalInfo("extra details");
		detailedContactInfo.setDepartment(detailedDepartment);


		SimpleDto simpleDto1 = SimpleDto.builder()
				.name("hari")
				.address(Address.builder()
						.city("BR")
						.street("MR")
						.zipCode("560037")
						.build())
				.contactInfo(basicContactInfo)
				.build();


		SimpleDto simpleDto2 = SimpleDto.builder()
				.name("hari")
				.address(Address.builder()
						.city("BR")
						.street("MR")
						.zipCode("560037")
						.build())
				.contactInfo(detailedContactInfo)
				.build();

        try {
            String json = new ObjectMapper().writeValueAsString(simpleDto1);
			System.out.println("simple json = " + json);
			String json2 = new ObjectMapper().writeValueAsString(simpleDto2);
			System.out.println("json2 = " + json2);


			SimpleDto jsonToClass = new ObjectMapper().readValue(json, SimpleDto.class);
			System.out.println("jsonToClass = " + jsonToClass);

			SimpleDto json2ToClass = new ObjectMapper().readValue(json2, SimpleDto.class);
			System.out.println("json2ToClass = " + json2ToClass);

		} catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

		// db test
		demoEntityRepo.save(DemoEntity.builder()
						.name("entity 1")
						.simpleDto(simpleDto1)
				.build());
		demoEntityRepo.save(DemoEntity.builder()
				.name("entity 2")
				.simpleDto(simpleDto2)
				.build());
		demoEntityRepo.findAll()
				.stream().peek(demoEntity -> {
					System.out.println("demoEntity.getSimpleDtoJson() = " + demoEntity.getSimpleDtoJson());
				})
				.forEach(demoEntity -> System.out.println("demoEntity = " + demoEntity));
    }
}

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SimpleDto {
	private String name;  // Normal field
	private Address address;  // Complex field

	// Step 2: Polymorphic Type Handling
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = BasicContactInfo.class, name = "basic"),
			@JsonSubTypes.Type(value = DetailedContactInfo.class, name = "detailed")
	})
	private ContactInfo contactInfo;  // Field as an interface
}

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Address {
	private String street;
	private String city;
	private String zipCode;
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = BasicContactInfo.class, name = "basic"),
		@JsonSubTypes.Type(value = DetailedContactInfo.class, name = "detailed")
})
interface ContactInfo {
	String getEmail();
	String getPhoneNumber();
}

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class BasicContactInfo implements ContactInfo {
	private String email;
	private String phoneNumber;
	private String address;
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = BasicDepartment.class, name = "basic"),
			@JsonSubTypes.Type(value = DetailedDepartment.class, name = "detailed")
	})
	private Department department;
}

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class DetailedContactInfo implements ContactInfo {
	private String email;
	private String phoneNumber;
	private String address;
	private String additionalInfo;
//	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//	@JsonSubTypes({
//			@JsonSubTypes.Type(value = BasicDepartment.class, name = "basic"),
//			@JsonSubTypes.Type(value = DetailedDepartment.class, name = "detailed")
//	})
	private Department department;
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = BasicDepartment.class, name = "basic"),
		@JsonSubTypes.Type(value = DetailedDepartment.class, name = "detailed")
})
interface Department {
	String getDepartmentName();
}

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BasicDepartment implements Department {
	private String departmentName;
	private String location;  // Additional field
}

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DetailedDepartment implements Department {
	@JsonProperty
	private String departmentName;
	@JsonProperty
	private int numberOfEmployees;  // Different additional field
}

// store in database


@ToString
@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
class DemoEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	String name;

	@Column(name = "json", columnDefinition = "TEXT")
	String simpleDtoJson;

	@Transient
	SimpleDto simpleDto;

	@PrePersist
	@PreUpdate
	public void serializeDtoClass_classToJson(){
		try {
            this.simpleDtoJson = new ObjectMapper().writeValueAsString(this.simpleDto);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@PostLoad
	public void deserializeDtoClass_jsonToClass(){
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			this.simpleDto = objectMapper.readValue(this.simpleDtoJson, SimpleDto.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

@Repository
interface DemoEntityRepo extends JpaRepository<DemoEntity, Long> {}
