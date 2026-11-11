-- =====================================================================================================================
--  Copyright (c) 2026 Volkswagen AG
--
--  See the NOTICE file(s) distributed with this work for additional
--  information regarding copyright ownership.
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0.
--
--  Unless required by applicable law or agreed to in writing, software
--  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
--  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
--  License for the specific language governing permissions and limitations
--  under the License.
--
--  AI Disclosure: This file was largely AI-generated. The AI-generated
--  portions are made available under CC0-1.0 and not subject to the
--  project's licence. The human contributor has reviewed and verified
--  that the code is correct.
--
--  SPDX-License-Identifier: Apache-2.0 AND CC0-1.0
--  Assisted-by: GitHub Copilot (Claude-Opus-5.5)
-- =====================================================================================================================

-- =====================================================================================================================
-- SQL functions for matching EDC assets & contract definitions
-- =====================================================================================================================

-- ---------------------------------------------------------------------------------------------------------------------
-- Function for parsing a property key into path segments
-- ---------------------------------------------------------------------------------------------------------------------
-- Mirrors EDC's PathItem#parse: dots separate segments, single quotes toggle a quoted section in which dots are part of
-- the segment. Quotes themselves are removed. Examples:
--    a.b      -> {a,b}
--    'a.b'    -> {a.b}
--    a.'b.c'  -> {a,b.c}
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_parse_path(key text)
RETURNS text[]
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    result  text[] := ARRAY[]::text[];
    segment text := '';
    quoted  boolean := false;
    ch      text;
BEGIN
    IF key IS NULL THEN
        RETURN NULL;
    END IF;

    -- shortcut for the common case: no dots, no quotes -> single segment
    IF position('.' IN key) = 0 AND position('''' IN key) = 0 THEN
        RETURN ARRAY[key];
    END IF;

    -- shortcut: no quotes -> every dot separates (the key is not empty here, as it contains a dot)
    IF position('''' IN key) = 0 THEN
        RETURN string_to_array(key, '.');
    END IF;

    -- shortcut: a key fully enclosed in quotes without further quotes -> single segment
    IF length(key) >= 2 AND left(key, 1) = '''' AND right(key, 1) = ''''
       AND position('''' IN substr(key, 2, length(key) - 2)) = 0 THEN
        RETURN ARRAY[substr(key, 2, length(key) - 2)];
    END IF;

    -- general case: parse the key character by character, toggling quoted mode on every quote to preserve anything
    -- inside quotes as a single segment, and otherwise splitting on dots
    FOREACH ch IN ARRAY regexp_split_to_array(key, '') LOOP
        IF ch = '''' THEN
            quoted := NOT quoted;
        ELSIF ch = '.' AND NOT quoted THEN
            result := result || segment;
            segment := '';
        ELSE
            segment := segment || ch;
        END IF;
    END LOOP;

    RETURN result || segment;
END;
$$;

COMMENT ON FUNCTION edc_parse_path(text) IS
    'Splits a property key into path segments like EDC''s PathItem#parse: dots separate segments, '
    'single quotes group a section containing dots into one segment.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for navigating a path within a JSONB object and extracting the value denominated by the path
-- ---------------------------------------------------------------------------------------------------------------------
-- Mirrors EDC's ReflectionUtil#getFieldValue for JSON-deserialized properties (maps, lists, scalars):
--    * an object is navigated by key
--    * a segment of the form name[i] selects element i of the list stored under name
--    * a list in the middle of the path yields no value: EDC maps every element via reflection, which fails for
--      deserialized JSON objects. The exception is a list that is empty or contains only nulls, which EDC maps to an
--      empty list.
--    * scalars in the middle of the path and JSON null values yield no value
-- Returns NULL if no value is found.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_get_path(obj jsonb, path text[])
RETURNS jsonb
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    current jsonb := obj;
    segment text;
    open_ix int;
    close_ix int;
BEGIN
    IF obj IS NULL OR path IS NULL THEN
        RETURN NULL;
    END IF;

    -- loops over all path segments and navigates the JSONB object accordingly, returning NULL if any segment cannot be resolved
    FOREACH segment IN ARRAY path LOOP
        IF current IS NULL OR jsonb_typeof(current) = 'null' THEN
            RETURN NULL;
        END IF;

        -- reached a list while segments remain: EDC maps the segment over every non-null element via reflection,
        -- which throws for deserialized JSON (no such Java field) -> no value. With no non-null elements nothing is
        -- mapped and EDC keeps returning an empty list, so the rest of the path can be skipped.
        IF jsonb_typeof(current) = 'array' THEN
            IF EXISTS (SELECT 1 FROM jsonb_array_elements(current) e WHERE jsonb_typeof(e.value) <> 'null') THEN
                RETURN NULL;
            END IF;
            RETURN '[]'::jsonb;
        END IF;

        -- a scalar (string, number, boolean) can't be navigated further
        IF jsonb_typeof(current) <> 'object' THEN
            RETURN NULL;
        END IF;

        -- array indexer, matched like ReflectionUtil's ARRAY_INDEXER_REGEX ".*\[([0-9])+\]"
        IF segment ~ '^.*\[[0-9]+\]$' THEN
            -- like ReflectionUtil, use the first '[' and first ']'; if they're in the wrong order, EDC fails -> no value
            open_ix := position('[' IN segment);
            close_ix := position(']' IN segment);
            IF close_ix < open_ix THEN
                RETURN NULL;
            END IF;
            -- look up the key before '['; it must hold a list (EDC casts it to List)
            current := current -> substring(segment FROM 1 FOR open_ix - 1);
            IF current IS NULL OR jsonb_typeof(current) <> 'array' THEN
                RETURN NULL;
            END IF;
            current := current -> substring(segment FROM open_ix + 1 FOR close_ix - open_ix - 1)::int;
        ELSE
            -- plain segment: look up the object key (EDC: Map#get)
            current := current -> segment;
        END IF;
    END LOOP;

    IF current IS NULL OR jsonb_typeof(current) = 'null' THEN
        RETURN NULL;
    END IF;
    RETURN current;
END;
$$;

COMMENT ON FUNCTION edc_get_path(jsonb, text[]) IS
    'Navigates a path represented as segments within a JSONB object like EDC''s ReflectionUtil#getFieldValue to get '
    'the value denominated by the path. Returns NULL if no value is found.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for asset property resolution
-- ---------------------------------------------------------------------------------------------------------------------
-- Mirrors EDC's AssetPropertyLookup, which returns the first non-null value of:
--    1. the key as path in properties            (e.g. a.b -> properties.a.b)
--    2. the whole key in properties              (e.g. a.b -> properties["a.b"])
--    3. the key as path in private properties
--    4. the whole key in private properties
--
-- Both readings are resolved against the effective (merged) properties. This is unambiguous, as EDC does not permit
-- the same top-level key in both properties and private properties. Only if both readings yield a value, the
-- private properties are consulted to determine which reading EDC would pick.
--
-- Not reproduced: EDC's final fallback to fields of the Asset object itself (e.g. createdAt, dataAddress).
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_resolve_property(props jsonb, private_props json, left_op text)
RETURNS jsonb
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    path_segments  text[];
    exact_segments text[];
    path_value     jsonb;
    exact_value    jsonb;
    private_keys   jsonb;
BEGIN
    IF props IS NULL OR left_op IS NULL THEN
        RETURN NULL;
    END IF;

    path_segments := edc_parse_path(left_op);
    -- EDC looks up the whole key by wrapping it in single quotes
    exact_segments := edc_parse_path('''' || left_op || '''');

    -- unambiguous case: both readings yield the same value
    path_value := edc_get_path(props, path_segments);
    IF path_segments = exact_segments THEN
        RETURN path_value;
    END IF;

    exact_value := edc_get_path(props, exact_segments);
    -- unambiguous case: only one reading yields a value
    IF path_value IS NULL OR exact_value IS NULL THEN
        RETURN COALESCE(path_value, exact_value);
    END IF;

    -- both readings yield a value: properties precede private properties, the path precedes the whole key
    private_keys := COALESCE(private_props::jsonb, '{}'::jsonb);
    IF private_keys ? path_segments[1] AND NOT private_keys ? exact_segments[1] THEN
        RETURN exact_value;
    END IF;
    RETURN path_value;
END;
$$;

COMMENT ON FUNCTION edc_resolve_property(jsonb, json, text) IS
    'Resolves a criterion''s leftOperand against an asset''s effective properties like EDC''s '
    'AssetPropertyLookup: path before whole key, properties before private properties.';

-- ---------------------------------------------------------------------------------------------------------------------
-- Function for stripping null values from a JSONB object
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_strip_nulls(obj jsonb)
RETURNS jsonb LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
SELECT COALESCE(
   (SELECT jsonb_object_agg(e.key, e.value)
    FROM jsonb_each(COALESCE(obj, '{}'::jsonb)) e
    WHERE jsonb_typeof(e.value) <> 'null'),
   '{}'::jsonb);
$$;

COMMENT ON FUNCTION edc_strip_nulls(jsonb) IS
    'Removes top-level null values from a JSONB object. Returns an empty object if the input is NULL or contains only '
    'nulls.';

-- ---------------------------------------------------------------------------------------------------------------------
-- Function for generating effective properties of an asset (properties merged over private properties)
-- ---------------------------------------------------------------------------------------------------------------------
-- Merges an asset's properties and private properties into a single JSONB object.
--
-- Top-level null values are removed from properties before merging to keep them out of the GIN index, as
-- AssetPropertyLookup filters on non-null values anyway.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_effective_properties(properties jsonb, private_properties jsonb)
RETURNS jsonb
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT edc_strip_nulls(private_properties) || edc_strip_nulls(properties);
$$;

COMMENT ON FUNCTION edc_effective_properties(jsonb, jsonb) IS
    'Merges an asset''s properties over its private properties.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Materialize the assets' effective properties as a generated column
-- ---------------------------------------------------------------------------------------------------------------------
-- By storing effective properties in a singular column, they become GIN-indexable and can be used in the fast path
-- of edc_find_matching_asset_ids and edc_find_authorized_asset.
--
-- By defining the column to be GENERATED ALWAYS, Postgres keeps it in sync on every insert and update. As EDC
-- explicitly addresses all columns when writing to or reading from the table, the additional column is simply ignored
-- there.
--
-- For prefilled tables, adding this column will compute the effective properties for all existing rows, locking the
-- table for the duration of the computation. Therefore, on the initial run of this script, the asset table will be
-- locked for a few seconds. As the script is executed during EDC start-up - before any API requests are made - this
-- should not cause any issues.
-- ---------------------------------------------------------------------------------------------------------------------
-- NOTE: changing the definition of edc_effective_properties does NOT recompute the stored values. The column has to be
-- dropped and added again for a changed definition to apply.
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE edc_asset
    ADD COLUMN IF NOT EXISTS effective_properties jsonb
        GENERATED ALWAYS AS (edc_effective_properties(properties::jsonb, private_properties::jsonb)) STORED;

COMMENT ON COLUMN edc_asset.effective_properties IS
    'Derived column maintained by Postgres: properties merged over private properties. '
    'Backs the GIN index used by the matching fast path, never read from or written to by EDC.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for comparing two JSONB strings or numbers using a specific operator
-- ---------------------------------------------------------------------------------------------------------------------
-- Mirrors EDC's NumberStringOperatorPredicate: only two numbers or two strings are comparable, every other type
-- combination yields false.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_number_string_compare(a jsonb, b jsonb, op text)
RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT CASE
        WHEN a IS NULL OR b IS NULL THEN false
        WHEN jsonb_typeof(a) = 'number' AND jsonb_typeof(b) = 'number' THEN
            CASE op
                -- '{}' is the empty path literal, causing #>> to return the top-level scalar itself as text
                WHEN '='  THEN (a #>> '{}')::numeric =  (b #>> '{}')::numeric
                WHEN '<'  THEN (a #>> '{}')::numeric <  (b #>> '{}')::numeric
                WHEN '<=' THEN (a #>> '{}')::numeric <= (b #>> '{}')::numeric
                WHEN '>'  THEN (a #>> '{}')::numeric >  (b #>> '{}')::numeric
                WHEN '>=' THEN (a #>> '{}')::numeric >= (b #>> '{}')::numeric
                ELSE false
            END
        WHEN jsonb_typeof(a) = 'string' AND jsonb_typeof(b) = 'string' THEN
            CASE op
                -- strings are compared using the C collation to mirror the order produced by Java's natural-order String comparator
                WHEN '='  THEN (a #>> '{}') =  (b #>> '{}')
                WHEN '<'  THEN (a #>> '{}') COLLATE "C" <  (b #>> '{}') COLLATE "C"
                WHEN '<=' THEN (a #>> '{}') COLLATE "C" <= (b #>> '{}') COLLATE "C"
                WHEN '>'  THEN (a #>> '{}') COLLATE "C" >  (b #>> '{}') COLLATE "C"
                WHEN '>=' THEN (a #>> '{}') COLLATE "C" >= (b #>> '{}') COLLATE "C"
                ELSE false
            END
        ELSE false
    END;
$$;

COMMENT ON FUNCTION edc_number_string_compare(jsonb, jsonb, text) IS
    'Mirrors EDC''s NumberStringOperatorPredicate: compares two numbers or two strings, '
    'returns false for any other type combination (objects, arrays, booleans, mixed types).';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for comparing two JSONB values for equality
-- ---------------------------------------------------------------------------------------------------------------------
-- Mirrors EDC's EqualOperatorPredicate: parses a boolean before comparison, checks membership in case the property is
-- an array, and else defaults to number/string comparison.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_equal_matches(a jsonb, b jsonb)
RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT CASE
        -- Boolean: EqualOperatorPredicate parses the right operand via Boolean.parseBoolean(toString()),
        -- a null right operand short-circuits to false
        WHEN jsonb_typeof(a) = 'boolean' THEN
            CASE
                WHEN b IS NULL OR jsonb_typeof(b) = 'null' THEN false
                ELSE COALESCE(lower(b #>> '{}') = 'true', false) = (a = 'true'::jsonb)
            END
        -- List: EqualOperatorPredicate checks membership, not equality of the whole list
        WHEN jsonb_typeof(a) = 'array' THEN
            EXISTS (SELECT 1 FROM jsonb_array_elements(a) e WHERE e.value = b)
        -- Object/Map: falls through to NumberStringOperatorPredicate in EqualOperatorPredicate,
        -- as it is neither a number nor a string => always false
        ELSE edc_number_string_compare(a, b, '=')
    END;
$$;

COMMENT ON FUNCTION edc_equal_matches(jsonb, jsonb) IS
    'Mirrors EDC''s EqualOperatorPredicate: boolean parsing, list membership, number/string '
    'comparison. Object-valued properties never match.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for evaluating a single criterion against an asset's properties
-- ---------------------------------------------------------------------------------------------------------------------
-- Resolves the property to evaluate using the criterion's left operand, and then applies different operations for
-- comparing the resolved property against the criterion's right operand, depending on the criterion's operator.
--
-- If the resolved property is null, the criterion is considered not matched, regardless of the operator. This is in
-- line with the behavior of EDC's CriterionOperatorRegistryImpl.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_criterion_matches(
    props         jsonb,
    private_props json,
    left_op       text,
    operator      text,
    right_op      jsonb
) RETURNS boolean
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    actual jsonb;
    a_txt  text;
    r_txt  text;
    op     text := lower(trim(operator));
BEGIN
    actual := edc_resolve_property(props, private_props, left_op);

    -- CriterionOperatorRegistryImpl#toPredicate returns false if the property lookup yields null, regardless of the operator
    IF actual IS NULL OR jsonb_typeof(actual) = 'null' THEN
        RETURN false;
    END IF;

    IF op IN ('<', '<=', '>', '>=') THEN
        RETURN edc_number_string_compare(actual, right_op, op);
    END IF;

    a_txt := CASE WHEN jsonb_typeof(actual)   = 'string' THEN actual   #>> '{}' ELSE actual::text   END;
    r_txt := CASE WHEN jsonb_typeof(right_op) = 'string' THEN right_op #>> '{}' ELSE right_op::text END;

    RETURN CASE op
        WHEN '='  THEN edc_equal_matches(actual, right_op)
        WHEN '!=' THEN NOT edc_equal_matches(actual, right_op)

        -- InOperatorPredicate uses Objects.equals against every element of the right operand
        WHEN 'in' THEN CASE
            WHEN jsonb_typeof(right_op) = 'array' THEN EXISTS (
                SELECT 1 FROM jsonb_array_elements(right_op) e WHERE e.value = actual)
            ELSE false
        END

        WHEN 'not in' THEN CASE
            WHEN jsonb_typeof(right_op) = 'array' THEN NOT EXISTS (
                SELECT 1 FROM jsonb_array_elements(right_op) e WHERE e.value = actual)
            ELSE false
        END

        -- ContainsOperatorPredicate only accepts Collection properties
        WHEN 'contains' THEN CASE
            WHEN jsonb_typeof(actual) = 'array' THEN EXISTS (
                SELECT 1 FROM jsonb_array_elements(actual) e WHERE e.value = right_op)
            ELSE false
        END

        -- EDC applies like/ilike on property.toString(), but only if rightOperand is
        -- a string. For scalars the JSON rendering matches Java's toString(); objects and
        -- arrays render differently in Java and are therefore not reproducible here.
       WHEN 'like'  THEN jsonb_typeof(right_op) = 'string' AND a_txt LIKE  r_txt
       WHEN 'ilike' THEN jsonb_typeof(right_op) = 'string' AND a_txt ILIKE r_txt
       ELSE false
    END;
END;
$$;

COMMENT ON FUNCTION edc_criterion_matches(jsonb, json, text, text, jsonb) IS
    'Tests a single asset selector criterion against an asset''s properties. '
    'Supports =, !=, in, not in, <, <=, >, >=, contains, like, ilike. '
    'Follows the semantics of EDC''s CriterionOperatorRegistryImpl and its predicates.';

-- ---------------------------------------------------------------------------------------------------------------------
-- Function for rendering path segments as jsonpath accessor
-- ---------------------------------------------------------------------------------------------------------------------
-- {a,b.c} -> @."a"."b.c"
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_jsonpath_accessor(segments text[])
RETURNS text
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    result  text := '@';
    segment text;
BEGIN
    IF segments IS NULL OR cardinality(segments) = 0 THEN
        RETURN NULL;
    END IF;

    FOREACH segment IN ARRAY segments LOOP
        result := result || '.' || to_json(segment)::text; -- to_json quotes and escapes the segment
    END LOOP;

    RETURN result;
END;
$$;

COMMENT ON FUNCTION edc_jsonpath_accessor(text[]) IS
    'Renders a path represented as segments into a jsonpath accessor, e.g. {a,b.c} -> @."a"."b.c".';

-- ---------------------------------------------------------------------------------------------------------------------
-- Function for splitting a contract definition's asset selector into fast-path (=, in) and the rest
-- ---------------------------------------------------------------------------------------------------------------------
-- The asset selector consists of a list of criteria, each with operandLeft, operator and operandRight.
--
-- Criteria with operator "=" and a scalar right operand (string, number, boolean) are combined into a single jsonpath
-- predicate (eq_filter), which is GIN-indexable via "@?". Criteria with operator "in" and a non-empty list of scalar
-- right operands additionally contribute a disjunction (@.p == v1 || @.p == v2 || ...) to eq_filter.
--
-- jsonpath is used rather than "@>" because containment does NOT unwrap arrays at nested levels:
-- '{"a":["b"]}' @> '{"a":"b"}' is false, so assets with list-valued properties would be silently missed.
-- In lax mode '$.p ? (@ == "b")' unwraps the array and therefore reproduces the list-membership semantics of EDC's
-- EqualOperatorPredicate.
--
-- Keys containing dots or quotes can be read in two ways by EDC (see edc_resolve_property): as path (a.b -> a -> b)
-- and as whole key ("a.b"). eq_filter covers both readings with an OR, so no asset matched by EDC is missed. Keys
-- with array indexers (a[0]) are not eligible for eq_filter and are only evaluated exactly.
--
-- The returned columns:
--    eq_filter        -> GIN-indexable prefilter, never excludes an asset matched by EDC
--    ambiguous_filter -> matches assets for which eq_filter may disagree with EDC for an "=" criterion, i.e. assets
--                        where both readings of a key yield a value, or where a path crosses a list (lax mode unwraps
--                        it, EDC yields no value). NULL if no "=" criterion can be ambiguous.
--    eq_criteria      -> the "=" criteria covered by ambiguous_filter, to be evaluated exactly for those assets
--    other_criteria   -> all criteria not covered exactly by eq_filter, including "in" criteria (eq_filter is only a
--                        superset for them), to be evaluated exactly for every candidate asset
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_split_selector(
    assets_selector jsonb,
    OUT eq_filter        jsonpath,
    OUT ambiguous_filter jsonpath,
    OUT eq_criteria      jsonb,
    OUT other_criteria   jsonb
)
-- PL/pgSQL rather than SQL: the function is not inlinable (it returns a record), and PL/pgSQL caches the plans of its
-- statements, whereas a SQL function would be planned again for every contract definition
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE
    criterion      jsonb;
    lhs            text;
    op             text;
    rhs            jsonb;
    path_segments  text[];
    exact_segments text[];
    readings       text[];  -- distinct readings of the key, rendered as jsonpath accessors (e.g. @."a"."b")
    accessor       text;
    indexable      boolean;
    is_eq          boolean;
    is_in          boolean;
    disjuncts      text[];
    eq_predicates  text[] := ARRAY[]::text[];
    in_predicates  text[] := ARRAY[]::text[];
    guard_parts    text[] := ARRAY[]::text[]; -- parts of a disjunction guarding "=" criteria for which eq_filter may disagree with EDC
    k              int;
BEGIN
    eq_criteria := '[]'::jsonb;
    other_criteria := '[]'::jsonb;

    FOR criterion IN
        SELECT c.value
        FROM jsonb_array_elements(COALESCE(assets_selector, '[]'::jsonb)) WITH ORDINALITY AS c(value, ix)
        ORDER BY c.ix
    LOOP
        lhs := criterion ->> 'operandLeft';
        op := lower(trim(criterion ->> 'operator'));
        rhs := criterion -> 'operandRight';
        -- the two readings of the key, see edc_resolve_property
        path_segments := edc_parse_path(lhs);
        exact_segments := edc_parse_path('''' || lhs || '''');

        -- keys with array indexers (a[0]) are not expressed in jsonpath
        indexable := lhs IS NOT NULL
                     AND NOT EXISTS (SELECT 1
                                     FROM unnest(path_segments || exact_segments) s(segment)
                                     WHERE s.segment ~ '^.*\[[0-9]+\]$');

        -- selects criteria with operator "=" and a scalar right operand (string, number, boolean)
        is_eq := COALESCE(indexable AND op = '=' AND jsonb_typeof(rhs) IN ('string', 'number', 'boolean'), false);

        -- selects criteria with operator "in" and a non-empty list of scalar right operands
        is_in := false;
        IF indexable AND op = 'in' AND jsonb_typeof(rhs) = 'array' THEN
            is_in := jsonb_array_length(rhs) > 0
                     AND NOT EXISTS (SELECT 1
                                     FROM jsonb_array_elements(rhs) e
                                     WHERE jsonb_typeof(e.value) NOT IN ('string', 'number', 'boolean'));
        END IF;

        -- collects jsonpath accessors for the two readings of the key (path vs. exact key match), if they are different
        IF is_eq OR is_in THEN
            readings := ARRAY[edc_jsonpath_accessor(path_segments)];
            IF exact_segments <> path_segments THEN
                readings := readings || edc_jsonpath_accessor(exact_segments);
            END IF;
        END IF;

        IF is_eq THEN
            -- builds a jsonpath expression for the "=" criterion, ORing the readings of its key
            disjuncts := ARRAY[]::text[];
            FOREACH accessor IN ARRAY readings LOOP
                disjuncts := disjuncts || (accessor || ' == ' || rhs::text);
                IF jsonb_typeof(rhs) <> 'boolean' THEN
                    -- for non-boolean rightOperands a second disjunct reproduces the Boolean branch of
                    -- EDC's EqualOperatorPredicate, which compares a boolean property against
                    -- Boolean.parseBoolean(operandRight.toString()). This keeps selectors such as
                    -- `active = "true"` matching an asset property `active: true`.
                    disjuncts := disjuncts || (accessor || ' == '
                        || CASE WHEN lower(criterion ->> 'operandRight') = 'true' THEN 'true' ELSE 'false' END);
                END IF;
            END LOOP;

            eq_predicates := eq_predicates || ('(' || array_to_string(disjuncts, ' || ') || ')');

            -- "=" criteria for which eq_filter may disagree with EDC: two readings, or a path crossing a nested level
            IF exact_segments <> path_segments OR cardinality(path_segments) > 1 THEN
                eq_criteria := eq_criteria || jsonb_build_array(criterion);

                -- if both readings yield a value, the criterion needs to be evaluated exactly for those assets
                IF exact_segments <> path_segments THEN
                    guard_parts := guard_parts || ('exists(' || edc_jsonpath_accessor(path_segments) || ') && exists('
                                                   || edc_jsonpath_accessor(exact_segments) || ')');
                END IF;

                -- a list in the middle of a path: lax mode unwraps it, EDC yields no value
                FOR k IN 1 .. cardinality(path_segments) - 1 LOOP
                    guard_parts := guard_parts
                                   || (edc_jsonpath_accessor(path_segments[1:k]) || '.type() == "array"');
                END LOOP;
                IF exact_segments <> path_segments THEN
                    FOR k IN 1 .. cardinality(exact_segments) - 1 LOOP
                        guard_parts := guard_parts
                                       || (edc_jsonpath_accessor(exact_segments[1:k]) || '.type() == "array"');
                    END LOOP;
                END IF;
            END IF;
        ELSE
            -- all criteria not covered exactly by eq_filter, including "in" criteria
            other_criteria := other_criteria || jsonb_build_array(criterion);
        END IF;

        IF is_in THEN
            -- builds a prefilter disjunction (@.p == v1 || @.p == v2 || ...) for the "in" criterion
            SELECT array_agg(r.accessor || ' == ' || e.value::text ORDER BY r.ix, e.ix)
            INTO disjuncts
            FROM unnest(readings) WITH ORDINALITY AS r(accessor, ix)
            CROSS JOIN LATERAL jsonb_array_elements(rhs) WITH ORDINALITY AS e(value, ix);
            in_predicates := in_predicates || ('(' || array_to_string(disjuncts, ' || ') || ')');
        END IF;
    END LOOP;

    -- joins all predicates with "&&" into a top-level filter, or returns the root path if there are none
    IF cardinality(eq_predicates || in_predicates) > 0 THEN
        eq_filter := ('$ ? (' || array_to_string(eq_predicates || in_predicates, ' && ') || ')')::jsonpath;
    ELSE
        eq_filter := '$'::jsonpath;
    END IF;

    -- constructs an ambiguity-filter out of the guard_parts: for assets matching this filter "="-criteria need to be re-evaluated
    IF cardinality(guard_parts) > 0 THEN
        ambiguous_filter := ('$ ? (' || array_to_string(
            ARRAY(SELECT '(' || g.part || ')' FROM unnest(guard_parts) WITH ORDINALITY AS g(part, ix) ORDER BY g.ix),
            ' || ') || ')')::jsonpath;
    ELSE
        ambiguous_filter := NULL;
    END IF;
END;
$$;

COMMENT ON FUNCTION edc_split_selector(jsonb) IS
    'Splits an asset selector into a GIN-indexable jsonpath prefilter (eq_filter) covering "=" and '
    '"in" criteria with scalar operands, a jsonpath (ambiguous_filter) matching assets for which '
    'eq_filter may disagree with EDC, the "=" criteria to evaluate exactly for those assets '
    '(eq_criteria), and all remaining criteria to evaluate exactly (other_criteria).';


-- ---------------------------------------------------------------------------------------------------------------------
-- Store the split selectors on the contract definition table
-- ---------------------------------------------------------------------------------------------------------------------
-- The split only depends on the asset selector, so it is computed once when a contract definition is written instead
-- of on every catalog request. The columns are maintained by a trigger; EDC addresses all columns explicitly when
-- writing to the table and reads them by name, so the additional columns are ignored there.
--
-- A trigger is used rather than GENERATED ALWAYS columns, because a generated column depends on edc_split_selector:
--    * the function could no longer be dropped and recreated by this script
--    * changing the function would not recompute the stored values
-- Instead, the stored values are refreshed below whenever this script changes the result of edc_split_selector.
--
-- Columns that can never be NULL are declared NOT NULL. "CREATE TEMPORARY TABLE ... (LIKE edc_contract_definitions)"
-- copies these constraints (but not the trigger), so a temporary table that is filled without the split selectors
-- fails loudly instead of silently matching no assets.
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE edc_contract_definitions
    ADD COLUMN IF NOT EXISTS selector_eq_filter        jsonpath,
    ADD COLUMN IF NOT EXISTS selector_ambiguous_filter jsonpath,
    ADD COLUMN IF NOT EXISTS selector_eq_criteria      jsonb,
    ADD COLUMN IF NOT EXISTS selector_other_criteria   jsonb;

CREATE OR REPLACE FUNCTION edc_contract_definitions_split_selector()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    SELECT s.eq_filter, s.ambiguous_filter, s.eq_criteria, s.other_criteria
    INTO NEW.selector_eq_filter, NEW.selector_ambiguous_filter, NEW.selector_eq_criteria, NEW.selector_other_criteria
    FROM edc_split_selector(NEW.assets_selector::jsonb) s;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION edc_contract_definitions_split_selector() IS
    'Trigger function keeping the selector_* columns of edc_contract_definitions in sync with assets_selector.';

-- fires on every update (not only of assets_selector), so the stored values cannot be overwritten manually
CREATE OR REPLACE TRIGGER edc_contract_definitions_split_selector -- only supported for Postgres version >= 14
    BEFORE INSERT OR UPDATE ON edc_contract_definitions
    FOR EACH ROW EXECUTE FUNCTION edc_contract_definitions_split_selector();

-- (re)computes the stored values of rows written before the trigger existed or before edc_split_selector changed;
-- rows that are already up to date are not written (jsonpath has no equality operator, hence the text comparison)
UPDATE edc_contract_definitions t
SET assets_selector = t.assets_selector -- the trigger recomputes the selector_* columns
FROM edc_contract_definitions c
CROSS JOIN LATERAL edc_split_selector(c.assets_selector::jsonb) s
WHERE c.contract_definition_id = t.contract_definition_id
  AND (t.selector_eq_filter::text, t.selector_ambiguous_filter::text, t.selector_eq_criteria, t.selector_other_criteria)
      IS DISTINCT FROM (s.eq_filter::text, s.ambiguous_filter::text, s.eq_criteria, s.other_criteria);

ALTER TABLE edc_contract_definitions
    ALTER COLUMN selector_eq_filter SET NOT NULL,
    ALTER COLUMN selector_eq_criteria SET NOT NULL,
    ALTER COLUMN selector_other_criteria SET NOT NULL;

COMMENT ON COLUMN edc_contract_definitions.selector_eq_filter IS
    'Derived column maintained by trigger: eq_filter of edc_split_selector(assets_selector). Never written by EDC.';
COMMENT ON COLUMN edc_contract_definitions.selector_ambiguous_filter IS
    'Derived column maintained by trigger: ambiguous_filter of edc_split_selector(assets_selector). Never written by EDC.';
COMMENT ON COLUMN edc_contract_definitions.selector_eq_criteria IS
    'Derived column maintained by trigger: eq_criteria of edc_split_selector(assets_selector). Never written by EDC.';
COMMENT ON COLUMN edc_contract_definitions.selector_other_criteria IS
    'Derived column maintained by trigger: other_criteria of edc_split_selector(assets_selector). Never written by EDC.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Function for checking criteria exactly
-- ---------------------------------------------------------------------------------------------------------------------
-- Returns true if no criterion is violated. Each criterion is evaluated by edc_criterion_matches.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_selector_matches(props jsonb, private_props json, criteria jsonb)
RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
    SELECT NOT EXISTS (
        SELECT 1
        FROM jsonb_array_elements(COALESCE(criteria, '[]'::jsonb)) AS c(value)
        WHERE NOT edc_criterion_matches(
                props,
                private_props,
                c.value ->> 'operandLeft',
                c.value ->> 'operator',
                c.value -> 'operandRight')
    );
$$;

COMMENT ON FUNCTION edc_selector_matches(jsonb, json, jsonb) IS
    'Tests criteria of an asset selector against an asset''s effective properties using '
    'edc_criterion_matches. Returns true if none of the criteria are violated.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Bulk case, step 1: find IDs of matching assets
-- ---------------------------------------------------------------------------------------------------------------------
-- Drives the join from the contract definitions table and probes the GIN index on edc_asset.effective_properties
-- for every definition. Returns only asset IDs and matching definition IDs, so the result materialized by PL/pgSQL
-- stays small. Dynamic SQL (and therefore PL/pgSQL) is required, as the definitions table is passed by name.
--
-- The asset lookup is a LATERAL subquery with OFFSET 0, which forces one GIN index probe per definition. Without it,
-- the planner flattens the lookup into a regular join on participant_context_id: as it can neither estimate the
-- selectivity of "@?" with a non-constant jsonpath nor its real evaluation cost, it would then evaluate "@?" for every
-- asset x definition pair.
-- ---------------------------------------------------------------------------------------------------------------------
-- Parameters:
--    p_definitions_table -> table from which to read contract definitions
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_find_matching_asset_ids(p_definitions_table text)
RETURNS TABLE (
    asset_id                varchar,
    matching_definition_ids varchar[]
)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN QUERY EXECUTE format($q$
        WITH cd AS MATERIALIZED (
            -- reads the split selectors stored on the contract definitions (see edc_contract_definitions_split_selector)
            SELECT t.contract_definition_id,
                   t.participant_context_id,
                   t.selector_eq_filter        AS eq_filter,
                   t.selector_ambiguous_filter AS ambiguous_filter,
                   t.selector_eq_criteria      AS eq_criteria,
                   t.selector_other_criteria   AS other_criteria
            FROM %s t
        )
        SELECT a.asset_id,
               array_agg(DISTINCT cd.contract_definition_id)::varchar[]
        FROM cd
        CROSS JOIN LATERAL (
            -- fast-path: probes the GIN index once per contract definition
            SELECT x.asset_id,
                   x.effective_properties AS props,
                   x.private_properties AS private_props
            FROM edc_asset x
            WHERE x.participant_context_id = cd.participant_context_id
              AND x.effective_properties @? cd.eq_filter
            -- OFFSET 0 prevents the planner from flattening this subquery into a regular join
            OFFSET 0
        ) a
        -- "=" criteria are only evaluated exactly for assets on which eq_filter may disagree with EDC
        WHERE (cd.ambiguous_filter IS NULL
               OR NOT (a.props @? cd.ambiguous_filter)
               OR edc_selector_matches(a.props, a.private_props, cd.eq_criteria))
          AND edc_selector_matches(a.props, a.private_props, cd.other_criteria) -- evaluates the remaining criteria exactly
        GROUP BY a.asset_id
        $q$, quote_ident(p_definitions_table)); -- renders the table name as a safe SQL identifier
END;
$$;

COMMENT ON FUNCTION edc_find_matching_asset_ids(text) IS
    'Returns the IDs of all assets selected by at least one contract definition of the specified '
    'table, including the IDs of the matching definitions for each asset.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Bulk case, step 2: entry point for catalog queries
-- ---------------------------------------------------------------------------------------------------------------------
-- Joins the matching asset IDs with the asset table. The function is inlined by the planner, so the WHERE, ORDER BY
-- and LIMIT/OFFSET clauses of the calling query are planned together with the join instead of being applied to a
-- materialized result.
--
-- Inlining requires ALL of the following. Breaking any of them silently reintroduces full materialization:
--   * LANGUAGE sql (never plpgsql)
--   * STABLE or IMMUTABLE (not VOLATILE, the default)
--   * not STRICT, not SECURITY DEFINER, no SET clauses
--   * the body consists of a single SELECT
--   * the function is called in the FROM clause
--
-- Ordering is deliberately NOT defined here: an ORDER BY inside the function would only order a subquery, which
-- Postgres does not guarantee to preserve for the LIMIT/OFFSET of the calling query. The caller must define a total
-- order (e.g. ORDER BY asset_id) to page deterministically.
-- ---------------------------------------------------------------------------------------------------------------------
-- Parameters:
--    p_definitions_table -> table from which to read contract definitions
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_find_matching_assets_page(p_definitions_table text)
RETURNS TABLE (
    asset_id                varchar,
    created_at              bigint,
    properties              json,
    private_properties      json,
    data_address            json,
    participant_context_id  varchar,
    dataplane_metadata      json,
    matching_definition_ids varchar[]
)
LANGUAGE sql STABLE AS $$
    SELECT x.asset_id,
           x.created_at,
           x.properties,
           x.private_properties,
           x.data_address,
           x.participant_context_id,
           x.dataplane_metadata,
           m.matching_definition_ids
    FROM edc_find_matching_asset_ids(p_definitions_table) m
    JOIN edc_asset x ON x.asset_id = m.asset_id;
$$;

COMMENT ON FUNCTION edc_find_matching_assets_page(text) IS
    'Returns all assets selected by at least one contract definition of the specified table, '
    'including the matching definition IDs. Inlinable SQL function: filters, ordering and paging '
    'of the calling query are applied to the join directly.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Single-asset case
-- ---------------------------------------------------------------------------------------------------------------------
-- Resolves the asset via its primary key first and only then evaluates the selectors of the contract
-- definitions. Returns at most one row, so materialization by PL/pgSQL is irrelevant here.
-- ---------------------------------------------------------------------------------------------------------------------
-- Parameters:
--    p_definitions_table -> table from which to read contract definitions
--    p_asset_id -> ID of the asset to resolve
-- ---------------------------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION edc_find_authorized_asset(p_definitions_table text, p_asset_id text)
RETURNS TABLE (
    asset_id                varchar,
    created_at              bigint,
    properties              json,
    private_properties      json,
    data_address            json,
    participant_context_id  varchar,
    dataplane_metadata      json,
    matching_definition_ids varchar[]
)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN QUERY EXECUTE format($q$
        WITH a AS MATERIALIZED (
            SELECT x.*,
                   x.effective_properties AS props
            FROM edc_asset x
            WHERE x.asset_id = $1
        ),
        cd AS MATERIALIZED (
            -- reads the split selectors stored on the contract definitions (see edc_contract_definitions_split_selector)
            SELECT t.contract_definition_id,
                   t.participant_context_id,
                   t.selector_eq_filter        AS eq_filter,
                   t.selector_ambiguous_filter AS ambiguous_filter,
                   t.selector_eq_criteria      AS eq_criteria,
                   t.selector_other_criteria   AS other_criteria
            FROM %s t
        ),
        matched AS (
            SELECT array_agg(DISTINCT cd.contract_definition_id)::varchar[] AS matching_definition_ids
            FROM a
            JOIN cd
              ON cd.participant_context_id = a.participant_context_id
             AND a.props @? cd.eq_filter -- fast-path
            WHERE (cd.ambiguous_filter IS NULL
                   OR NOT (a.props @? cd.ambiguous_filter)
                   OR edc_selector_matches(a.props, a.private_properties, cd.eq_criteria)) -- ambiguous "=" criteria
              AND edc_selector_matches(a.props, a.private_properties, cd.other_criteria) -- remaining criteria
        )
        SELECT a.asset_id,
               a.created_at,
               a.properties,
               a.private_properties,
               a.data_address,
               a.participant_context_id,
               a.dataplane_metadata,
               m.matching_definition_ids
        FROM a
        CROSS JOIN matched m
        WHERE m.matching_definition_ids IS NOT NULL
        $q$, quote_ident(p_definitions_table)) -- renders the table name as a safe SQL identifier
    USING p_asset_id;
END;
$$;

COMMENT ON FUNCTION edc_find_authorized_asset(text, text) IS
    'Returns the asset with the given ID including the IDs of all contract definitions of the '
    'specified table that select it, or no row if the asset does not exist or is not selected.';


-- ---------------------------------------------------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------------------------------------------------
-- GIN index on the effective_properties column to speed up the "@?" fast-path of edc_find_matching_asset_ids and
-- edc_find_authorized_asset.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_edc_asset_effective_props_gin
    ON edc_asset USING gin (effective_properties jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_edc_asset_pcid
    ON edc_asset (participant_context_id);
