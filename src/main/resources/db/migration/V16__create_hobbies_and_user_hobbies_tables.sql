CREATE TABLE hobby (
    hobby_id UUID PRIMARY KEY,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE learner_hobby (
    learner_id UUID NOT NULL REFERENCES learners(learner_id) ON DELETE CASCADE,
    hobby_id UUID NOT NULL REFERENCES hobby(hobby_id),
    PRIMARY KEY (learner_id, hobby_id)
);

CREATE INDEX idx_learner_hobby_hobby_id ON learner_hobby(hobby_id);
