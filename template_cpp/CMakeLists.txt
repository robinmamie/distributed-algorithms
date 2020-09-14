cmake_minimum_required(VERSION 3.9)
project(da_project)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

string(CONCAT CMAKE_CXX_FLAGS_COMMON_STR ""
                        "-Werror -Wall -Wconversion -Wfloat-equal "
                        "-Wpedantic -Wpointer-arith -Wswitch-default "
                        "-Wpacked -Wextra -Winvalid-pch "
                        "-Wmissing-field-initializers "
                        "-Wunreachable-code -Wcast-align -Wcast-qual "
                        "-Wdisabled-optimization -Wformat=2 "
                        "-Wformat-nonliteral -Wuninitialized "
                        "-Wformat-security -Wformat-y2k -Winit-self "
                        "-Wmissing-declarations -Wmissing-include-dirs "
                        "-Wredundant-decls -Wstrict-overflow=5 -Wundef "
                        "-Wno-unused -Wctor-dtor-privacy -Wsign-promo "
                        "-Woverloaded-virtual -Wold-style-cast")

if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    string(CONCAT CMAKE_CXX_FLAGS_STR "${CMAKE_CXX_FLAGS_COMMON_STR} "
                        "-Wlogical-op -Wstrict-null-sentinel -Wnoexcept")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_STR}")
elseif (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    string(CONCAT CMAKE_CXX_FLAGS_STR "${CMAKE_CXX_FLAGS_COMMON_STR} ")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_STR}")
endif()

string(CONCAT CMAKE_C_FLAGS_COMMON_STR ""
                        "-Werror -Wall -Wconversion -Wfloat-equal "
                        "-Wpedantic -Wpointer-arith -Wswitch-default "
                        "-Wpacked -Wextra -Winvalid-pch "
                        "-Wmissing-field-initializers -Wunreachable-code "
                        "-Wcast-align -Wcast-qual -Wdisabled-optimization "
                        "-Wformat=2 -Wformat-nonliteral -Wuninitialized "
                        "-Wformat-security -Wformat-y2k -Winit-self "
                        "-Wmissing-declarations -Wmissing-include-dirs "
                        "-Wredundant-decls -Wstrict-overflow=5 "
                        "-Wundef -Wno-unused")

if (CMAKE_C_COMPILER_ID STREQUAL "GNU")
    string(CONCAT CMAKE_C_FLAGS_STR "${CMAKE_C_FLAGS_COMMON_STR} "
                        "-Wlogical-op")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS_STR}")
elseif (CMAKE_C_COMPILER_ID MATCHES "Clang")
    string(CONCAT CMAKE_C_FLAGS_STR "${CMAKE_C_FLAGS_COMMON_STR} ")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS_STR}")
endif()



set(CMAKE_C_FLAGS_DEBUG "-Winline -g")
set(CMAKE_C_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_C_FLAGS_RELWITHDEBINFO "-O2 -g -DNDEBUG")
set(CMAKE_CXX_FLAGS_MINSIZEREL "-Os -DNDEBUG")

set(CMAKE_CXX_FLAGS_DEBUG "-Winline -g")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_RELWITHDEBINFO "-O2 -g -DNDEBUG")
set(CMAKE_CXX_FLAGS_MINSIZEREL "-Os -DNDEBUG")

# MESSAGE( STATUS "CMAKE_C_FLAGS: " ${CMAKE_C_FLAGS} )
# MESSAGE( STATUS "CMAKE_CXX_FLAGS: " ${CMAKE_CXX_FLAGS} )
# MESSAGE( STATUS "CMAKE_BUILD_TYPE: " ${CMAKE_BUILD_TYPE} )

add_subdirectory(src)
